/* service-worker.js
 * PWA Service Worker for SmartPOS.
 * Caches static assets and product API responses for offline reliability.
 */

importScripts('/js/idb-helper.js');

const CACHE_NAME = 'smartpos-v2';
const STATIC_ASSETS = [
    '/',
    '/login.html',
    '/sales.html',
    '/products.html',
    '/dashboard.html',
    '/css/smartpos.css',
    '/css/rtl.css',
    '/js/smartpos.js',
    '/js/translations.js',
    '/js/idb-helper.js',
    'https://fonts.googleapis.com/css2?family=Inter:wght@400;500;600;700&display=swap',
    'https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.0/css/all.min.css'
];

self.addEventListener('install', event => {
    event.waitUntil(
        caches.open(CACHE_NAME)
            .then(cache => cache.addAll(STATIC_ASSETS))
            .then(() => self.skipWaiting())
    );
});

self.addEventListener('activate', event => {
    event.waitUntil(
        caches.keys().then(keys => {
            return Promise.all(
                keys.filter(key => key !== CACHE_NAME)
                    .map(key => caches.delete(key))
            );
        })
    );
});

self.addEventListener('fetch', event => {
    const url = new URL(event.request.url);

    // Offline transaction queuing
    if (event.request.method === 'POST' && url.pathname === '/api/sales/transaction') {
        event.respondWith((async () => {
            try {
                return await fetch(event.request.clone());
            } catch (err) {
                // Offline fallback
                const payload = await event.request.clone().json();
                
                // Add Authorization header info if available so background sync can use it later
                const authHeader = event.request.headers.get('Authorization') || '';
                const bizId = event.request.headers.get('X-Business-Id') || '';
                const branchId = event.request.headers.get('X-Branch-Id') || '';
                
                payload._headers = {
                    'Authorization': authHeader,
                    'X-Business-Id': bizId,
                    'X-Branch-Id': branchId
                };
                
                const offlineId = await IDB.savePendingSale(payload);
                
                // Fake successful response
                return new Response(JSON.stringify({
                    id: offlineId,
                    receiptNumber: offlineId,
                    offlineMode: true,
                    message: "Saved offline. Will sync when online."
                }), { 
                    status: 200, 
                    headers: { 'Content-Type': 'application/json' } 
                });
            }
        })());
        return;
    }

    // Offline supply templates preview fallback
    if (event.request.method === 'POST' && url.pathname.includes('/api/supply-templates/preview')) {
        event.respondWith((async () => {
            try {
                return await fetch(event.request.clone());
            } catch (err) {
                // Just return empty array to skip deductions step in offline mode
                return new Response(JSON.stringify([]), {
                    status: 200,
                    headers: { 'Content-Type': 'application/json' }
                });
            }
        })());
        return;
    }

    // Skip non-GET requests for SW caching
    if (event.request.method !== 'GET') return;

    // Network-First for API requests, Cache-First for static assets
    if (url.pathname.startsWith('/api/')) {
        event.respondWith(
            fetch(event.request)
                .catch(() => caches.match(event.request))
        );
    } else {
        event.respondWith(
            caches.match(event.request)
                .then(cachedResponse => cachedResponse || fetch(event.request))
        );
    }
});

// Background Sync
self.addEventListener('sync', event => {
    if (event.tag === 'sync-sales') {
        event.waitUntil(syncPendingSales());
    }
});

async function syncPendingSales() {
    const sales = await IDB.getPendingSales();
    if (!sales || sales.length === 0) return;

    for (const sale of sales) {
        try {
            const headers = new Headers({
                'Content-Type': 'application/json'
            });
            if (sale._headers) {
                if (sale._headers['Authorization']) headers.append('Authorization', sale._headers['Authorization']);
                if (sale._headers['X-Business-Id']) headers.append('X-Business-Id', sale._headers['X-Business-Id']);
                if (sale._headers['X-Branch-Id']) headers.append('X-Branch-Id', sale._headers['X-Branch-Id']);
            }

            const cleanPayload = { ...sale };
            delete cleanPayload._headers;
            delete cleanPayload.id; // DB auto-incremented id
            delete cleanPayload.offlineId;
            delete cleanPayload.timestamp;

            const response = await fetch('/api/sales/transaction', {
                method: 'POST',
                headers: headers,
                body: JSON.stringify(cleanPayload)
            });

            if (response.ok) {
                await IDB.deletePendingSale(sale.id);
            } else {
                await IDB.logSyncConflict({ saleId: sale.id, responseStatus: response.status });
            }
        } catch (err) {
            console.error('Failed to sync sale', sale.id, err);
            // Will throw to retry next time
            throw err;
        }
    }
}

// Allow frontend to trigger sync via postMessage
self.addEventListener('message', event => {
    if (event.data && event.data.action === 'sync-sales') {
        syncPendingSales().then(() => {
            event.ports[0].postMessage({ success: true });
        }).catch(err => {
            event.ports[0].postMessage({ success: false, error: err.message });
        });
    }
});
