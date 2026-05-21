/* idb-helper.js
 * Simplified wrapper for IndexedDB to handle offline Sales and Product Caching.
 * Version 1.1.0 (Fixed & Robust)
 */

const IDB = {
    DB_NAME: 'SmartPOS_OfflineDB',
    DB_VERSION: 1,
    STORES: {
        PENDING_SALES:   'pending_sales',
        CACHED_PRODUCTS: 'cached_products',
        SYNC_LOGS:       'sync_logs'
    },
    db: null,

    /**
     * Internal: Gets or opens the database connection.
     */
    async _getDB() {
        if (this.db) return this.db;
        return new Promise((resolve, reject) => {
            const request = indexedDB.open(this.DB_NAME, this.DB_VERSION);
            request.onerror = () => reject(request.error);
            request.onsuccess = () => {
                this.db = request.result;
                resolve(request.result);
            };
            request.onupgradeneeded = (e) => {
                const db = e.target.result;
                // Pending sales store (Autoincrement ID)
                if (!db.objectStoreNames.contains(this.STORES.PENDING_SALES)) {
                    db.createObjectStore(this.STORES.PENDING_SALES, { keyPath: 'id', autoIncrement: true });
                }
                // Product cache store (Standard ID)
                if (!db.objectStoreNames.contains(this.STORES.CACHED_PRODUCTS)) {
                    db.createObjectStore(this.STORES.CACHED_PRODUCTS, { keyPath: 'id' });
                }
                // Sync conflict logs
                if (!db.objectStoreNames.contains(this.STORES.SYNC_LOGS)) {
                    db.createObjectStore(this.STORES.SYNC_LOGS, { keyPath: 'id', autoIncrement: true });
                }
            };
        });
    },

    /**
     * Saves a sale to the offline queue.
     */
    async savePendingSale(saleData) {
        const db = await this._getDB();
        return new Promise((resolve, reject) => {
            const tx = db.transaction(this.STORES.PENDING_SALES, 'readwrite');
            const store = tx.objectStore(this.STORES.PENDING_SALES);
            const entry = {
                ...saleData,
                offlineId: `OFF_TX_${Date.now()}_${Math.random().toString(36).substr(2, 4).toUpperCase()}`,
                timestamp: new Date().toISOString()
            };
            const request = store.add(entry);
            request.onsuccess = () => resolve(entry.offlineId);
            request.onerror   = () => reject(request.error);
        });
    },

    /**
     * Gets all sales currently waiting to be synced.
     */
    async getPendingSales() {
        const db = await this._getDB();
        return new Promise((resolve, reject) => {
            const tx = db.transaction(this.STORES.PENDING_SALES, 'readonly');
            const store = tx.objectStore(this.STORES.PENDING_SALES);
            const request = store.getAll();
            request.onsuccess = () => resolve(request.result);
            request.onerror   = () => reject(request.error);
        });
    },

    /**
     * Deletes a sale from the queue after successful sync.
     */
    async deletePendingSale(id) {
        const db = await this._getDB();
        return new Promise((resolve, reject) => {
            const tx = db.transaction(this.STORES.PENDING_SALES, 'readwrite');
            const store = tx.objectStore(this.STORES.PENDING_SALES);
            const request = store.delete(id);
            request.onsuccess = () => resolve();
            request.onerror   = () => reject(request.error);
        });
    },

    /**
     * Caches a fresh list of products for offline lookup.
     */
    async cacheProducts(products) {
        if (!Array.isArray(products)) return;
        const db = await this._getDB();
        return new Promise((resolve, reject) => {
            const tx = db.transaction(this.STORES.CACHED_PRODUCTS, 'readwrite');
            const store = tx.objectStore(this.STORES.CACHED_PRODUCTS);
            
            // Clear existing cache first
            store.clear();

            // Add new items
            products.forEach(p => {
                // Ensure p has an id for the keyPath
                if (p.id) store.put(p);
            });

            tx.oncomplete = () => resolve();
            tx.onerror    = () => reject(tx.error);
        });
    },

    /**
     * Retrieves the locally cached product list.
     */
    async getCachedProducts() {
        const db = await this._getDB();
        return new Promise((resolve, reject) => {
            const tx = db.transaction(this.STORES.CACHED_PRODUCTS, 'readonly');
            const store = tx.objectStore(this.STORES.CACHED_PRODUCTS);
            const request = store.getAll();
            request.onsuccess = () => resolve(request.result);
            request.onerror   = () => reject(request.error);
        });
    },

    /**
     * Logs a detailed sync failure for later review.
     */
    async logSyncConflict(logEntry) {
        const db = await this._getDB();
        return new Promise((resolve) => {
            const tx = db.transaction(this.STORES.SYNC_LOGS, 'readwrite');
            const store = tx.objectStore(this.STORES.SYNC_LOGS);
            store.add({
                ...logEntry,
                loggedAt: new Date().toISOString()
            });
            tx.oncomplete = () => resolve();
        });
    }
};
