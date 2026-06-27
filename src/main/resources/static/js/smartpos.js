/* ============================================================
   SmartPOS Shared JavaScript — Version 2.5
   Changes in 2.5:
   - JWT authentication: token stored in sessionStorage,
     sent as Authorization: Bearer header on every API call
   - Auto-redirect to login on 401 (expired/invalid token)
   Changes in 2.4:
   - Sidebar scroll memory
   - Fixed RoleGuard.hiddenNav.CASHIER missing stockout + suppliers
   ============================================================ */

// ── CONFIG ───────────────────────────────────────────────────
const SmartPOS = {
    version: '2.5.0',
    api: {
        base:       '',
        products:   '/api/products',
        sales:      '/api/sales',
        admin:      '/api/admin',
        businesses: '/api/admin/businesses',
        auth:       '/api/auth'
    }
};

// ── AUTH ─────────────────────────────────────────────────────
const Auth = {

    setAdmin(data) {
        sessionStorage.setItem('sp_admin_id',   data.adminId);
        sessionStorage.setItem('sp_admin_name', data.fullName);
        sessionStorage.setItem('sp_role',       'admin');
    },

    setClient(data) {
        // Store JWT token
        if (data.token) {
            sessionStorage.setItem('sp_token', data.token);
        }
        sessionStorage.setItem('sp_user_id',       data.userId);
        sessionStorage.setItem('sp_user_name',     data.fullName);
        sessionStorage.setItem('sp_user_email',    data.email);
        sessionStorage.setItem('sp_user_role',     data.role);
        sessionStorage.setItem('sp_business_id',   data.businessId);
        sessionStorage.setItem('sp_business_name', data.businessName);
        sessionStorage.setItem('sp_plan',          data.plan);
        sessionStorage.setItem('sp_role',          'client');
        // Store branches from login response for branch-select.html
        if (data.branches) {
            sessionStorage.setItem('sp_branches',
                JSON.stringify(data.branches));
        }
        // Branch context — set after branch selection
        if (data.branchId) {
            sessionStorage.setItem('sp_branch_id',   data.branchId);
            sessionStorage.setItem('sp_branch_name', data.branchName);
        }
    },

    setBranch(branchId, branchName) {
        sessionStorage.setItem('sp_branch_id',   branchId);
        sessionStorage.setItem('sp_branch_name', branchName);
    },

    clearBranch() {
        sessionStorage.removeItem('sp_branch_id');
        sessionStorage.removeItem('sp_branch_name');
    },

    isAdmin()    { return sessionStorage.getItem('sp_role') === 'admin'; },
    isClient()   { return sessionStorage.getItem('sp_role') === 'client'; },
    isLoggedIn() { return this.isAdmin() || this.isClient(); },

    getAdminName()    { return sessionStorage.getItem('sp_admin_name')    || 'Admin'; },
    getUserName()     { return sessionStorage.getItem('sp_user_name')     || 'User'; },
    getUserEmail()    { return sessionStorage.getItem('sp_user_email')    || ''; },
    getUserId()       { return sessionStorage.getItem('sp_user_id')       || null; },
    getUserRole()     { return sessionStorage.getItem('sp_user_role')     || 'CASHIER'; },
    getBusinessId()   { return sessionStorage.getItem('sp_business_id'); },
    getBusinessName() { return sessionStorage.getItem('sp_business_name') || 'SmartPOS'; },
    getPlan()         { return sessionStorage.getItem('sp_plan')          || 'BASIC'; },
    getBranchId()     { return sessionStorage.getItem('sp_branch_id')     || null; },
    getBranchName()   { return sessionStorage.getItem('sp_branch_name')   || null; },
    getToken()        { return sessionStorage.getItem('sp_token')         || null; },
    isOwnerGlobal()   {
        // Owner viewing all branches (no branch selected)
        return this.getUserRole() === 'OWNER' && !this.getBranchId();
    },

    requireAdmin() {
        if (!this.isAdmin()) {
            window.location.href = '/admin-login.html';
            return false;
        }
        return true;
    },

    requireClient() {
        if (!this.isClient()) {
            console.warn('Access denied: User is not a client.');
            sessionStorage.setItem('sp_access_denied', '1');
            window.location.href = '/login.html';
            return false;
        }
        return true;
    },

    logout() {
        const isAdmin = this.isAdmin();
        sessionStorage.clear();
        window.location.href = isAdmin ? '/admin-login.html' : '/login.html';
    },

    clientLogout() { this.logout(); }
};

// ── PLAN GUARD ───────────────────────────────────────────────
const PlanGuard = {

    access: {
        BASIC: [
            '/dashboard.html', '/sales.html',
            '/products.html', '/archived-products.html',
            '/staff.html', '/stock-adjustments.html',
            '/stockout.html', '/parties.html', '/suppliers.html',
            '/purchase-orders.html', '/returns.html',
            '/supplies.html', '/stock-transfers.html',
            '/branch-select.html', '/branches.html',
            '/cash-sheet.html'
        ],
        BUSINESS: [
            '/dashboard.html', '/sales.html',
            '/products.html', '/archived-products.html',
            '/staff.html', '/stock-adjustments.html',
            '/stockout.html', '/parties.html', '/suppliers.html',
            '/purchase-orders.html', '/returns.html',
            '/supplies.html', '/stock-transfers.html',
            '/branch-select.html', '/branches.html',
            '/report.html', '/expenses.html', '/customers.html',
            '/cash-sheet.html'
        ],
        ENTERPRISE: [
            '/dashboard.html', '/sales.html',
            '/products.html', '/archived-products.html',
            '/staff.html', '/stock-adjustments.html',
            '/stockout.html', '/parties.html', '/suppliers.html',
            '/purchase-orders.html', '/returns.html',
            '/supplies.html', '/stock-transfers.html',
            '/branch-select.html', '/branches.html',
            '/report.html', '/expenses.html', '/customers.html',
            '/cash-sheet.html'
        ]
    },

    limits: {
        BASIC:      { staff: 3,         products: 200,      branches: 1 },
        BUSINESS:   { staff: 10,        products: Infinity, branches: 3 },
        ENTERPRISE: { staff: Infinity,  products: Infinity, branches: Infinity }
    },

    featureNames: {
        '/report.html':    'Business Reports',
        '/expenses.html':  'Expense Tracking',
        '/customers.html': 'Customer Management'
    },

    requiredPlan: {
        '/report.html':    'BUSINESS',
        '/expenses.html':  'BUSINESS',
        '/customers.html': 'BUSINESS'
    },

    canAccess(page) {
        const plan    = Auth.getPlan();
        const allowed = this.access[plan] || this.access.BASIC;
        return allowed.includes(page);
    },

    withinLimit(resource, currentCount) {
        const plan  = Auth.getPlan();
        const limit = (this.limits[plan] || this.limits.BASIC)[resource];
        return currentCount < limit;
    },

    getLimit(resource) {
        const plan = Auth.getPlan();
        return (this.limits[plan] || this.limits.BASIC)[resource];
    },

    guardCurrentPage() {
        const page = window.location.pathname;
        if (!this.canAccess(page)) {
            sessionStorage.setItem('sp_plan_blocked_page',    page);
            sessionStorage.setItem('sp_plan_blocked_feature',
                this.featureNames[page] || 'This feature');
            sessionStorage.setItem('sp_plan_required',
                this.requiredPlan[page] || 'BUSINESS');
            window.location.href = '/upgrade.html';
            return false;
        }
        return true;
    },

    applyToSidebar() {
        const all = ['/report.html', '/expenses.html', '/customers.html'];

        all.forEach(href => {
            const link = document.querySelector(`.nav-item[href="${href}"]`);
            if (!link) return;

            if (!this.canAccess(href)) {
                link.style.opacity  = '0.5';
                link.style.cursor   = 'pointer';
                link.removeAttribute('href');
                link.setAttribute('data-locked-href', href);
                link.onclick = () => {
                    sessionStorage.setItem('sp_plan_blocked_page',    href);
                    sessionStorage.setItem('sp_plan_blocked_feature',
                        PlanGuard.featureNames[href] || 'This feature');
                    sessionStorage.setItem('sp_plan_required',
                        PlanGuard.requiredPlan[href] || 'BUSINESS');
                    window.location.href = '/upgrade.html';
                };

                const label = link.querySelector('.nav-label');
                if (label && !label.querySelector('.plan-lock')) {
                    label.insertAdjacentHTML('beforeend',
                        '<i class="fas fa-lock plan-lock" style="font-size:0.6rem;margin-left:auto;opacity:0.7"></i>');
                }
            }
        });
    }
};

// ── ROLE GUARD ───────────────────────────────────────────────
const RoleGuard = {

    access: {
        OWNER: [
            '/dashboard.html', '/sales.html', '/sales-list.html', '/sales-return.html',
            '/products.html', '/archived-products.html', '/report.html', '/staff.html',
            '/expenses.html', '/customers.html', '/parties.html', '/suppliers.html',
            '/stock-adjustments.html', '/stockout.html',
            '/predictions.html',
            '/purchase-orders.html',
            '/returns.html', '/purchase-return.html', '/supplies.html',
            '/stock-transfers.html', '/branches.html',
            '/branch-select.html', '/cash-sheet.html',
            '/purchase.html', '/purchase-list.html',
            '/statement.html', '/metal-fix.html'
        ],
        MANAGER: [
            '/dashboard.html', '/sales.html', '/sales-list.html', '/sales-return.html',
            '/products.html', '/archived-products.html', '/report.html',
            '/expenses.html', '/customers.html', '/parties.html', '/suppliers.html',
            '/stock-adjustments.html', '/stockout.html',
            '/predictions.html',
            '/purchase-orders.html',
            '/returns.html', '/purchase-return.html', '/supplies.html',
            '/stock-transfers.html', '/branch-select.html',
            '/cash-sheet.html',
            '/purchase.html', '/purchase-list.html',
            '/statement.html', '/metal-fix.html'
        ],
        CASHIER: [
            '/dashboard.html', '/sales.html', '/sales-list.html', '/branch-select.html',
            '/cash-sheet.html', '/parties.html', '/suppliers.html',
            '/purchase.html', '/purchase-list.html', '/purchase-return.html',
            '/statement.html'
        ]
    },

    hiddenNav: {
        CASHIER: [
            '/products.html', '/archived-products.html',
            '/report.html', '/staff.html',
            '/expenses.html', '/customers.html',
            '/stock-adjustments.html', '/stockout.html',
            '/predictions.html',
            '/purchase-orders.html',
            '/returns.html', '/supplies.html',
            '/stock-transfers.html', '/branches.html'
        ],
        MANAGER: [ '/staff.html', '/branches.html' ],
        OWNER:   []
    },

    guardCurrentPage() {
        if (!Auth.isClient()) {
            window.location.href = '/login.html';
            return false;
        }
        const role    = Auth.getUserRole();
        const page    = window.location.pathname;
        const allowed = this.access[role] || this.access.CASHIER;

        if (!allowed.includes(page)) {
            sessionStorage.setItem('sp_access_denied', '1');
            window.location.href = '/dashboard.html';
            return false;
        }
        return true;
    },

    applyToSidebar() {
        const role   = Auth.getUserRole();
        const hidden = this.hiddenNav[role] || [];
        hidden.forEach(href => {
            const link = document.querySelector(`.nav-item[href="${href}"]`);
            if (link) link.style.display = 'none';
        });

        if (hidden.includes('/staff.html')) {
            document.querySelectorAll('.nav-group-label').forEach(label => {
                if (label.textContent.trim() === 'Team')
                    label.style.display = 'none';
            });
        }
    },

    checkAccessDenied() {
        if (sessionStorage.getItem('sp_access_denied') === '1') {
            sessionStorage.removeItem('sp_access_denied');
            Toast.warning('You do not have permission to access that page.');
        }
    }
};

// ── API HELPER ───────────────────────────────────────────────
const API = {

    _headers() {
        const headers = { 'Content-Type': 'application/json' };
        const token = Auth.getToken();
        if (token) headers['Authorization'] = 'Bearer ' + token;
        
        const bizId    = Auth.getBusinessId();
        const branchId = Auth.getBranchId();
        
        // Critical context check for clients
        if (Auth.isClient() && !bizId) {
            console.error('CRITICAL: Business ID missing from session.');
            // We don't throw here to avoid infinite loops in some pages, 
            // but the backend will likely return 400.
        }

        if (bizId)    headers['X-Business-Id'] = bizId;
        if (branchId) headers['X-Branch-Id']   = branchId;

        const userId = Auth.getUserId();
        if (userId)   headers['X-User-Id']     = userId;
        
        // Debug headers in dev environments if needed
        // console.debug('API Request Headers:', headers);
        
        return headers;
    },

    _handle401(res) {
        if (res.status === 401) {
            const isAdmin = Auth.isAdmin();
            sessionStorage.clear();
            Toast.warning('Session expired. Please log in again.');
            setTimeout(() => {
                window.location.href = isAdmin ? '/admin-login.html' : '/login.html';
            }, 1200);
            throw new Error('Session expired');
        }
    },

    async _parseResponse(res) {
        if (res.status === 204) return null;
        const text = await res.text().catch(() => '');
        if (!text || !text.trim()) return null;
        try {
            return JSON.parse(text);
        } catch {
            // For non-JSON success responses, return the raw text to avoid crashing
            if (res.ok) return text;
            return { message: text || `Request failed (${res.status})`, status: res.status };
        }
    },

    async get(url) {
        try {
            const res = await fetch(url, { headers: this._headers() });
            this._handle401(res);
            if (!res.ok) throw await this._parseResponse(res);
            let data = await this._parseResponse(res);
            
            // Auto-fallback to empty array for list-like endpoints if body is empty
            const isListUrl = url.includes('/api/') && !url.includes('summary') && !url.includes('check');
            if (!data && isListUrl) data = [];
            else if (data === null) data = {}; 

            // Task 11: Cache products locally
            if (url.includes('/api/products') && typeof IDB !== 'undefined' && Array.isArray(data)) {
                IDB.cacheProducts(data).catch(e => console.error('Failed to cache products', e));
            }
            return data;
        } catch (err) {
            // Task 11: If products request fails, try cached data
            if (url.includes('/api/products') && typeof IDB !== 'undefined') {
                const cached = await IDB.getCachedProducts();
                if (cached && cached.length > 0) {
                    console.info('Returning cached product data.');
                    Toast.info('Operating in offline mode (cached data).');
                    return cached;
                }
            }
            
            // Critical Diagnostic: Show the ACTUAL error message to help the user/developer
            const msg = err.message || (typeof err === 'string' ? err : 'Unknown logic error');
            console.error(`API Fetch Error [${url}]:`, err);
            
            // Show toast only if it's not a products call (which has its own logic)
            if (!url.includes('/api/products')) {
                Toast.error(`Server Error: ${msg}`);
            }
            
            throw err;
        }
    },

    async post(url, data) {
        try {
            const res = await fetch(url, {
                method:  'POST',
                headers: this._headers(),
                body:    JSON.stringify(data)
            });
            this._handle401(res);
            if (!res.ok) throw await this._parseResponse(res);
            return (await this._parseResponse(res)) || {};
        } catch (err) {
            // Task 11: If network error and it is a sale, save to IndexedDB
            const isSale = url.includes('/api/sales') && !url.includes('/report');
            if (isSale && (err instanceof TypeError || !navigator.onLine)) {
                console.warn('Network error detected. Saving sale to offline queue.');
                if (typeof IDB !== 'undefined') {
                    const offlineId = await IDB.savePendingSale(data);
                    Toast.warning('Offline Mode: Sale saved locally. It will sync when reconnected.');
                    return { success: true, offline: true, id: offlineId, message: 'Saved offline' };
                }
            }
            throw err;
        }
    },

    async put(url, data) {
        const res = await fetch(url, {
            method:  'PUT',
            headers: this._headers(),
            body:    JSON.stringify(data)
        });
        this._handle401(res);
        if (!res.ok) throw await this._parseResponse(res);
        return (await this._parseResponse(res)) || {};
    },

    async delete(url) {
        const res = await fetch(url, {
            method:  'DELETE',
            headers: this._headers()
        });
        this._handle401(res);
        if (!res.ok) throw await this._parseResponse(res);
        // Ensure even empty success returns an object
        const data = await this._parseResponse(res);
        return data || {};
    }
};

// ── TOAST NOTIFICATIONS ──────────────────────────────────────
const Toast = {
    container: null,

    init() {
        if (!this.container) {
            this.container = document.createElement('div');
            this.container.className = 'toast-container';
            document.body.appendChild(this.container);
        }
    },

    show(message, type = 'info', duration = 3500) {
        this.init();
        const icons = {
            success: 'fa-check-circle',
            error:   'fa-exclamation-circle',
            info:    'fa-info-circle',
            warning: 'fa-exclamation-triangle'
        };
        const toast = document.createElement('div');
        toast.className = `toast toast-${type}`;
        toast.innerHTML = `
            <i class="fas ${icons[type] || icons.info}"></i>
            <span>${message}</span>
        `;
        this.container.appendChild(toast);
        setTimeout(() => {
            toast.classList.add('hiding');
            setTimeout(() => toast.remove(), 300);
        }, duration);
    },

    success(msg) { this.show(msg, 'success'); },
    error(msg)   { this.show(msg, 'error'); },
    info(msg)    { this.show(msg, 'info'); },
    warning(msg) { this.show(msg, 'warning'); }
};

// ── MODAL ────────────────────────────────────────────────────
const Modal = {
    open(id) {
        const el = document.getElementById(id);
        if (el) el.classList.add('open');
    },
    close(id) {
        const el = document.getElementById(id);
        if (el) el.classList.remove('open');
    },
    closeAll() {
        document.querySelectorAll('.modal-overlay')
            .forEach(m => m.classList.remove('open'));
    }
};

document.addEventListener('click', e => {
    if (e.target.classList.contains('modal-overlay')) Modal.closeAll();
});
document.addEventListener('keydown', e => {
    if (e.key === 'Escape') Modal.closeAll();
});

// ── THEME ────────────────────────────────────────────────────
const Theme = {
    STORAGE_KEY: 'sp_theme',

    // Returns the active theme.
    // Priority: manual override → OS preference (never hard-codes a default).
    get() {
        const stored = localStorage.getItem(this.STORAGE_KEY);
        if (stored) return stored;
        // No manual override — mirror the OS colour scheme
        return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
    },

    // True when the user has explicitly chosen a theme (not just following OS).
    isManual() {
        return localStorage.getItem(this.STORAGE_KEY) !== null;
    },

    apply() {
        const theme = this.get();
        // Mirror to 'smartpos_theme' so settings page can read it
        localStorage.setItem('smartpos_theme', theme);
        if (theme === 'light') {
            document.documentElement.setAttribute('data-theme', 'light');
        } else {
            document.documentElement.removeAttribute('data-theme');
        }
        // Dispatch so settings page appearance card can update
        window.dispatchEvent(new Event('themeChanged'));
    },

    toggle() {
        // Switching manually always records an explicit override
        const next = this.get() === 'dark' ? 'light' : 'dark';
        localStorage.setItem(this.STORAGE_KEY, next);
        this.apply();
    },

    init() {
        this.apply();
        // Re-apply automatically when the OS colour scheme changes,
        // but only while the user has not set a manual override.
        window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
            if (!this.isManual()) this.apply();
        });
    }
};

Theme.init();

// ── SIDEBAR ──────────────────────────────────────────────────
const Sidebar = {
    SCROLL_KEY: 'sp_sidebar_scroll',

    init() {
        // Toggle collapse
        const btn = document.getElementById('toggleBtn');
        if (btn) {
            btn.addEventListener('click', () => {
                document.getElementById('sidebar')?.classList.toggle('collapsed');
                document.getElementById('topbar')?.classList.toggle('shifted');
                document.getElementById('mainContent')?.classList.toggle('shifted');
            });
        }

        // Business name in brand
        const brandText = document.querySelector('.sidebar-brand-text');
        if (brandText) {
            if (Auth.isClient()) {
                brandText.textContent = Auth.getBusinessName();
            } else if (Auth.isAdmin()) {
                brandText.textContent = 'SmartPOS';
            }
        }

        // Apply role + plan guards
        if (Auth.isClient()) {
            RoleGuard.applyToSidebar();
            PlanGuard.applyToSidebar();

            // ── GLOBAL VIEW SIDEBAR FILTERING ─────────────────
            // When owner is in global view (no branch selected),
            // hide branch-specific nav items.
            if (Auth.isOwnerGlobal()) {
                const globalSafe = BranchGuard.globalSafe;
                document.querySelectorAll('.sidebar-nav a.nav-item').forEach(link => {
                    const href = link.getAttribute('href');
                    if (href && !globalSafe.includes(href)) {
                        link.style.display = 'none';
                    }
                });
            }

            // Hide nav group labels that have no visible items after filtering (Role, Plan, or Global View)
            document.querySelectorAll('.nav-group-label').forEach(label => {
                let next = label.nextElementSibling;
                let hasVisible = false;
                while (next && !next.classList.contains('nav-group-label')) {
                    if (next.classList.contains('nav-item') && next.style.display !== 'none') {
                        hasVisible = true;
                        break;
                    }
                    next = next.nextElementSibling;
                }
                if (!hasVisible) label.style.display = 'none';
                else label.style.display = 'block'; // Reset in case of dynamic role switches (rare)
            });
        }

        // Active state highlighting (Admins and Clients)
        const page = window.location.pathname;
        document.querySelectorAll('.sidebar-nav .nav-item, .sidebar-footer .nav-item').forEach(link => {
            if (link.getAttribute('href') === page) {
                link.classList.add('active');
            }
        });

        Theme.apply();

        // ── BRANCH CONTEXT IN TOPBAR ──────────────────────────
        // Shows current branch name next to page title
        // Owners get a "Switch Branch" button
        if (Auth.isClient()) {
            const branchName = Auth.getBranchName();
            const role       = Auth.getUserRole();
            const topbarLeft = document.querySelector('.topbar-left');

            if (topbarLeft) {
                // Add branch badge next to page title
                const existing = document.getElementById('branchBadge');
                if (!existing) {
                    const badge = document.createElement('div');
                    badge.id = 'branchBadge';
                    badge.style.cssText = `
                        display:inline-flex;align-items:center;gap:6px;
                        font-size:0.7rem;font-weight:700;
                        padding:4px 12px;border-radius:var(--radius-full);
                        background:rgba(99,102,241,0.12);
                        color:var(--primary);
                        border:1px solid rgba(99,102,241,0.25);
                        margin-left:12px;margin-right:12px;cursor:${role === 'OWNER' ? 'pointer' : 'default'};
                        white-space:nowrap;vertical-align:middle;
                    `;
                    badge.innerHTML = branchName
                        ? `<i class="fas fa-store" style="font-size:0.6rem"></i>
                           <span>${branchName}</span>`
                        : `<i class="fas fa-chart-pie" style="font-size:0.6rem"></i>
                           <span data-i18n="topbar_all_branches">All Branches</span>`;

                    if (role === 'OWNER') {
                        badge.title = 'Click to switch branch';
                        badge.addEventListener('click', () => {
                            window.location.href = '/branch-select.html';
                        });
                    }

                    const titleDiv = topbarLeft.querySelector('div');
                    if (titleDiv) {
                        // Badge always goes AFTER the title div.
                        // In LTR: [☰][title][badge] — badge on the right of title ✓
                        // In RTL: topbar-left uses flex-direction:row so same order,
                        //         group is pushed to the right side by margin-left:auto ✓
                        titleDiv.after(badge);
                    }
                }
            }
        }

        // ── SCROLL MEMORY ─────────────────────────────────────
        const nav = document.querySelector('.sidebar-nav');
        if (nav) {
            const saved = sessionStorage.getItem(this.SCROLL_KEY);
            if (saved !== null) {
                nav.scrollTop = parseInt(saved);
            } else {
                const activeItem = nav.querySelector('.nav-item.active');
                if (activeItem) {
                    activeItem.scrollIntoView({
                        block:    'center',
                        behavior: 'instant'
                    });
                }
            }

            nav.addEventListener('scroll', () => {
                sessionStorage.setItem(this.SCROLL_KEY, nav.scrollTop);
            });

            nav.querySelectorAll('a.nav-item').forEach(link => {
                link.addEventListener('click', () => {
                    sessionStorage.setItem(this.SCROLL_KEY, nav.scrollTop);
                });
            });
        }
    }
};

// ── AVATAR INITIALS ──────────────────────────────────────────
function setAvatarInitials(elementId, name) {
    const el = document.getElementById(elementId);
    if (!el || !name) return;
    const parts    = name.trim().split(' ');
    const initials = parts.length >= 2
        ? parts[0][0] + parts[parts.length - 1][0]
        : parts[0][0];
    el.textContent = initials.toUpperCase();
}

// ── FORMATTERS ───────────────────────────────────────────────
const Format = {
    currency(amount, currency = 'AED') {
        return `${currency} ${Number(amount || 0).toLocaleString('en-US', { minimumFractionDigits: 2, maximumFractionDigits: 2 })}`;
    },

    number(n) {
        return Number(n || 0).toLocaleString();
    },

    date(dateStr) {
        if (!dateStr) return '—';
        try {
            const d = new Date(dateStr);
            if (isNaN(d.getTime())) return '—';
            const day   = String(d.getDate()).padStart(2, '0');
            const month = String(d.getMonth() + 1).padStart(2, '0');
            const year  = d.getFullYear();
            return `${day}/${month}/${year}`;
        } catch { return '—'; }
    },

    datetime(dateStr) {
        if (!dateStr) return '—';
        try {
            const d = new Date(dateStr);
            if (isNaN(d.getTime())) return '—';
            const day   = String(d.getDate()).padStart(2, '0');
            const month = String(d.getMonth() + 1).padStart(2, '0');
            const year  = d.getFullYear();
            const hours = String(d.getHours()).padStart(2, '0');
            const mins  = String(d.getMinutes()).padStart(2, '0');
            return `${day}/${month}/${year} ${hours}:${mins}`;
        } catch { return '—'; }
    },

    planBadge(plan) {
        const map = {
            BASIC:      '<span class="badge badge-basic">Basic</span>',
            BUSINESS:   '<span class="badge badge-business">Business</span>',
            ENTERPRISE: '<span class="badge badge-enterprise">Enterprise</span>'
        };
        return map[plan] || `<span class="badge badge-neutral">${plan}</span>`;
    },

    statusBadge(status) {
        const map = {
            ACTIVE:    '<span class="badge badge-active">Active</span>',
            INACTIVE:  '<span class="badge badge-inactive">Inactive</span>',
            SUSPENDED: '<span class="badge badge-suspended">Suspended</span>'
        };
        return map[status] || `<span class="badge badge-neutral">${status}</span>`;
    },

    stockBadge(qty) {
        if (qty === 0)
            return '<span class="badge badge-out-stock">Out of Stock</span>';
        if (qty < 10)
            return `<span class="badge badge-low-stock">Low: ${qty}</span>`;
        return '<span class="badge badge-in-stock">In Stock</span>';
    }
};

// ── PLAN (legacy — kept for backward compatibility) ──────────
const Plan = {
    limits: {
        BASIC:      { branches: 1,        products: 200,      staff: 3  },
        BUSINESS:   { branches: 3,        products: Infinity, staff: 10 },
        ENTERPRISE: { branches: Infinity, products: Infinity, staff: Infinity }
    },

    features: {
        BASIC:      ['pos', 'products', 'inventory', 'dashboard'],
        BUSINESS:   ['pos', 'products', 'inventory', 'dashboard',
            'reports', 'staff', 'customers', 'expenses'],
        ENTERPRISE: ['pos', 'products', 'inventory', 'dashboard',
            'reports', 'staff', 'customers', 'expenses',
            'branches', 'ai', 'barcode', 'suppliers',
            'returns', 'api', 'branding']
    },

    can(feature) {
        const plan = Auth.getPlan();
        return (this.features[plan] || this.features.BASIC).includes(feature);
    },

    getLimit(resource) {
        const plan = Auth.getPlan();
        return (this.limits[plan] || this.limits.BASIC)[resource];
    },

    guard(feature, callback) {
        if (this.can(feature)) {
            callback();
        } else {
            Toast.warning(
                'This feature is not available on your current plan. ' +
                'Please upgrade to access it.'
            );
        }
    }
};

// ── BRANCH GUARD ─────────────────────────────────────────────
// Operation pages that require a branch context (POS, stock
// adjustments, supply management, etc.) call BranchGuard.require()
// at the top of their init. Owner in global view is redirected to
// branch-select. Non-owners always have a branch — this is a no-op.
const BranchGuard = {

    // Pages that work fine without a branch (owner global view)
    globalSafe: [
        '/dashboard.html', '/report.html', '/products.html',
        '/archived-products.html', '/branches.html',
        '/staff.html', '/branch-select.html', '/parties.html',
        '/expenses.html', '/predictions.html', '/customers.html'
    ],

    // Call this at the top of any page that requires a branch.
    // Returns true if a branch is set, false if redirecting.
    require() {
        if (!Auth.isClient()) return true;
        if (Auth.getBranchId()) return true;               // branch set — OK
        if (Auth.getUserRole() !== 'OWNER') return true;   // non-owner always has branch

        // Owner in global view on a page that needs a branch
        Toast.warning('Please select a branch for this operation.');
        setTimeout(() => {
            window.location.href = '/branch-select.html';
        }, 1200);
        return false;
    },

    // Check if the current page is safe for global view
    isCurrentPageGlobalSafe() {
        return this.globalSafe.includes(window.location.pathname);
    }
};

// ── NUMBER COUNTER ANIMATION ─────────────────────────────────
function animateCounter(element, end, duration = 1500) {
    if (!element) return;
    const start = 0;
    const isDecimal = String(end).includes('.');
    const decimals = isDecimal ? end.toString().split('.')[1].length : 0;

    let startTimestamp = null;
    const update = (timestamp) => {
        if (!startTimestamp) startTimestamp = timestamp;
        const progress = Math.min((timestamp - startTimestamp) / duration, 1);
        const current = progress * (end - start) + start;
        element.textContent = isDecimal
            ? current.toFixed(decimals)
            : Math.floor(current).toLocaleString();
        if (progress < 1) requestAnimationFrame(update);
    }
    requestAnimationFrame(update);
}

// ── SYNC MANAGER ─────────────────────────────────────────────
const SyncManager = {
    async init() {
        if (typeof IDB === 'undefined') return;

        window.addEventListener('online', () => this.sync());
        if (navigator.onLine) this.sync().catch(() => {});
    },

    async sync() {
        const pending = await IDB.getPendingSales();
        if (!pending || pending.length === 0) return;

        console.info(`SyncManager: Syncing ${pending.length} pending sales...`);
        let syncedCount = 0;
        for (const sale of pending) {
            try {
                const { offlineId, timestamp, id, _headers, ...cleanData } = sale;
                await API.post('/api/sales/transaction', cleanData);
                await IDB.deletePendingSale(sale.id);
                syncedCount++;
                console.info(`SyncManager: Successfully synced sale ${offlineId}`);
            } catch (err) {
                console.error(`SyncManager: Failed to sync sale ${sale.offlineId}`, err);
                if (err.status && err.status >= 400 && err.status !== 404) {
                    await IDB.logSyncConflict({ sale, error: err.message });
                    await IDB.deletePendingSale(sale.id);
                }
                break;
            }
        }
        // Only toast if at least one sale successfully synced
        if (syncedCount > 0) {
            Toast.success(`${syncedCount} offline sale${syncedCount !== 1 ? 's' : ''} synced.`);
        }
    }
};

// ── PASSWORD VISIBILITY TOGGLE ───────────────────────────────
// Auto-wraps every type="password" input with a show/hide eye button.
// Works on login, register, forgot-password, reset-password, profile modal.
function initPasswordToggles() {
    document.querySelectorAll('input[type="password"]').forEach(input => {
        // Skip if already wrapped
        if (input.dataset.pwToggleInit) return;
        input.dataset.pwToggleInit = 'true';

        // Ensure the parent is position:relative for button placement
        const wrap = input.parentElement;
        const wrapPos = window.getComputedStyle(wrap).position;
        if (wrapPos === 'static') wrap.style.position = 'relative';

        // Create toggle button
        const btn = document.createElement('button');
        btn.type      = 'button';
        btn.className = 'pw-toggle-btn';
        btn.innerHTML = '<i class="fas fa-eye"></i>';
        btn.setAttribute('tabindex', '-1');
        btn.setAttribute('aria-label', 'Show/hide password');

        btn.addEventListener('click', () => {
            const isHidden = input.type === 'password';
            input.type = isHidden ? 'text' : 'password';
            btn.innerHTML = isHidden
                ? '<i class="fas fa-eye-slash"></i>'
                : '<i class="fas fa-eye"></i>';
        });

        wrap.appendChild(btn);

        // Push input text away from the button
        const isRtl = document.documentElement.getAttribute('dir') === 'rtl';
        if (isRtl) {
            input.style.paddingLeft  = '36px';
        } else {
            input.style.paddingRight = '36px';
        }
    });
}

// ── PROFILE MODAL ────────────────────────────────────────────
const ProfileModal = {

    init() {
        if (!Auth.isClient()) return;
        // Make every avatar clickable
        document.querySelectorAll('.avatar[id]').forEach(el => {
            el.style.cursor = 'pointer';
            el.title = 'My Profile';
            el.onclick = () => ProfileModal.open();
        });
        this._inject();
    },

    _inject() {
        if (document.getElementById('profileModal')) return;
        const el = document.createElement('div');
        el.id = 'profileModal';
        el.className = 'modal-overlay';
        el.style.display = 'none';
        el.innerHTML = `
            <div class="modal" style="max-width:480px;width:100%">
                <div class="modal-header">
                    <span class="modal-title"><i class="fas fa-user-circle"></i> My Profile</span>
                    <button onclick="ProfileModal.close()" style="
                        width:32px;height:32px;border-radius:50%;border:none;
                        background:var(--surface-2);color:var(--text-muted);
                        display:flex;align-items:center;justify-content:center;
                        cursor:pointer;font-size:13px;transition:background 0.15s,color 0.15s;
                        flex-shrink:0;
                    " onmouseover="this.style.background='var(--danger-light,rgba(239,68,68,0.12))';this.style.color='#ef4444'"
                       onmouseout="this.style.background='var(--surface-2)';this.style.color='var(--text-muted)'">
                        <i class="fas fa-times"></i>
                    </button>
                </div>
                <div class="modal-body">

                    <!-- Avatar + name block -->
                    <div style="display:flex;align-items:center;gap:16px;margin-bottom:20px">
                        <div id="profileAvatar" style="width:56px;height:56px;border-radius:50%;background:var(--accent);color:#fff;display:flex;align-items:center;justify-content:center;font-size:1.5rem;font-weight:700;flex-shrink:0"></div>
                        <div>
                            <div id="profileName" style="font-size:var(--text-lg);font-weight:700;color:var(--text-primary)"></div>
                            <div id="profileEmail" style="font-size:var(--text-sm);color:var(--text-muted)"></div>
                        </div>
                    </div>

                    <!-- Info rows -->
                    <div style="background:var(--surface-2);border-radius:var(--radius-md);padding:14px 16px;display:flex;flex-direction:column;gap:10px;margin-bottom:20px">
                        <div style="display:flex;justify-content:space-between;align-items:center">
                            <span style="font-size:var(--text-sm);color:var(--text-muted)"><i class="fas fa-shield-halved" style="width:16px"></i> Role</span>
                            <span id="profileRole" style="font-size:var(--text-sm);font-weight:600;color:var(--text-primary)"></span>
                        </div>
                        <div style="display:flex;justify-content:space-between;align-items:center">
                            <span style="font-size:var(--text-sm);color:var(--text-muted)"><i class="fas fa-building" style="width:16px"></i> Business</span>
                            <span id="profileBusiness" style="font-size:var(--text-sm);font-weight:600;color:var(--text-primary)"></span>
                        </div>
                        <div style="display:flex;justify-content:space-between;align-items:center">
                            <span style="font-size:var(--text-sm);color:var(--text-muted)"><i class="fas fa-star" style="width:16px"></i> Plan</span>
                            <span id="profilePlan" style="font-size:var(--text-sm);font-weight:600;"></span>
                        </div>
                        <div id="profileBranchRow" style="display:flex;justify-content:space-between;align-items:center">
                            <span style="font-size:var(--text-sm);color:var(--text-muted)"><i class="fas fa-code-branch" style="width:16px"></i> Branch</span>
                            <span id="profileBranch" style="font-size:var(--text-sm);font-weight:600;color:var(--text-primary)"></span>
                        </div>
                    </div>

                    <!-- Change password section -->
                    <div style="border-top:1px solid var(--border);padding-top:16px">
                        <div style="font-size:var(--text-sm);font-weight:600;color:var(--text-primary);margin-bottom:12px">
                            <i class="fas fa-key"></i> Change Password
                        </div>
                        <div id="pwAlert" class="alert alert-error mb-3" style="display:none"></div>
                        <div id="pwSuccess" class="alert alert-success mb-3" style="display:none"></div>
                        <div style="display:flex;flex-direction:column;gap:10px">
                            <input type="password" id="pwCurrent" class="form-control" placeholder="Current password">
                            <input type="password" id="pwNew"     class="form-control" placeholder="New password (min 8 chars)">
                            <input type="password" id="pwConfirm" class="form-control" placeholder="Confirm new password">
                        </div>
                    </div>
                </div>
                <div class="modal-footer">
                    <button class="btn btn-ghost" onclick="ProfileModal.close()">Close</button>
                    <button class="btn btn-primary" id="pwSaveBtn" onclick="ProfileModal.changePassword()">
                        <i class="fas fa-save"></i> Save Password
                    </button>
                </div>
            </div>`;
        document.body.appendChild(el);
        // Wire up eye toggles for the dynamically-injected password fields
        setTimeout(() => initPasswordToggles(), 50);

        // Close on overlay click
        el.addEventListener('click', e => {
            if (e.target === el) ProfileModal.close();
        });
    },

    open() {
        const modal = document.getElementById('profileModal');
        if (!modal) return;

        // Populate fields from session
        const name     = Auth.getUserName();
        const email    = Auth.getUserEmail();
        const role     = Auth.getUserRole();
        const bizName  = Auth.getBusinessName();
        const plan     = Auth.getPlan();
        const branch   = Auth.getBranchName();
        const initial  = name ? name.charAt(0).toUpperCase() : '?';

        document.getElementById('profileAvatar').textContent  = initial;
        document.getElementById('profileName').textContent    = name;
        document.getElementById('profileEmail').textContent   = email;
        document.getElementById('profileRole').textContent    = role.charAt(0) + role.slice(1).toLowerCase();
        document.getElementById('profileBusiness').textContent = bizName;

        const planEl = document.getElementById('profilePlan');
        planEl.textContent = plan;
        planEl.style.color = plan === 'ENTERPRISE' ? '#f59e0b'
                           : plan === 'BUSINESS'   ? '#6366f1'
                           : 'var(--text-primary)';

        const branchRow = document.getElementById('profileBranchRow');
        if (branch) {
            document.getElementById('profileBranch').textContent = branch;
            branchRow.style.display = 'flex';
        } else {
            branchRow.style.display = 'none';
        }

        // Reset password fields
        ['pwCurrent','pwNew','pwConfirm'].forEach(id => {
            document.getElementById(id).value = '';
        });
        document.getElementById('pwAlert').style.display   = 'none';
        document.getElementById('pwSuccess').style.display = 'none';

        modal.style.display = 'flex';
        setTimeout(() => modal.classList.add('open'), 10);
    },

    close() {
        const modal = document.getElementById('profileModal');
        if (!modal) return;
        modal.classList.remove('open');
        setTimeout(() => { modal.style.display = 'none'; }, 200);
    },

    async changePassword() {
        const current = document.getElementById('pwCurrent').value;
        const newPw   = document.getElementById('pwNew').value;
        const confirm = document.getElementById('pwConfirm').value;
        const btn     = document.getElementById('pwSaveBtn');
        const errEl   = document.getElementById('pwAlert');
        const okEl    = document.getElementById('pwSuccess');

        errEl.style.display = 'none';
        okEl.style.display  = 'none';

        if (!current || !newPw || !confirm) {
            errEl.textContent   = 'Please fill in all password fields.';
            errEl.style.display = 'block';
            return;
        }
        if (newPw.length < 8) {
            errEl.textContent   = 'New password must be at least 8 characters.';
            errEl.style.display = 'block';
            return;
        }
        if (newPw !== confirm) {
            errEl.textContent   = 'Passwords do not match.';
            errEl.style.display = 'block';
            return;
        }

        btn.disabled = true;
        btn.innerHTML = '<i class="fas fa-spinner fa-spin"></i> Saving...';

        try {
            await API.post('/api/users/me/change-password', {
                currentPassword: current,
                newPassword:     newPw
            });
            okEl.textContent    = 'Password changed successfully!';
            okEl.style.display  = 'block';
            ['pwCurrent','pwNew','pwConfirm'].forEach(id => {
                document.getElementById(id).value = '';
            });
            Toast.success('Password updated.');
        } catch (e) {
            errEl.textContent   = e.message || 'Failed to change password.';
            errEl.style.display = 'block';
        } finally {
            btn.disabled = false;
            btn.innerHTML = '<i class="fas fa-save"></i> Save Password';
        }
    }
};

// ── NOTIFICATIONS ────────────────────────────────────────────
const Notifications = {
    _interval: null,
    _open: false,
    _items: [],

    init() {
        // Only run on authenticated client pages that have a topbar
        if (!Auth.isClient()) return;
        const topbarRight = document.querySelector('.topbar-right');
        if (!topbarRight) return;

        this._inject(topbarRight);
        this.refresh();
        // Auto-refresh every 60 seconds
        this._interval = setInterval(() => this.refresh(), 60000);
    },

    _inject(topbarRight) {
        // Remove old bell if present (dashboard already has one)
        const existing = document.getElementById('notifBtn');
        if (existing) {
            // Wrap existing bell so we can attach our dropdown to it
            existing.onclick = (e) => { e.stopPropagation(); Notifications.toggle(); };
        } else {
            // Inject bell before the avatar
            const avatar = topbarRight.querySelector('.avatar');
            const btn = document.createElement('button');
            btn.className = 'btn-toggle';
            btn.id = 'notifBtn';
            btn.title = 'Notifications';
            btn.innerHTML = '<i class="fas fa-bell"></i>';
            btn.onclick = (e) => { e.stopPropagation(); Notifications.toggle(); };
            topbarRight.insertBefore(btn, avatar);
        }

        // Inject dropdown into body (absolute positioned)
        if (!document.getElementById('notifDropdown')) {
            const el = document.createElement('div');
            el.id = 'notifDropdown';
            el.innerHTML = `
                <div class="notif-header">
                    <span class="notif-title">Notifications</span>
                    <span id="notifCount" class="notif-badge" style="display:none">0</span>
                </div>
                <div id="notifList" class="notif-list">
                    <div class="notif-empty"><i class="fas fa-check-circle"></i> All clear!</div>
                </div>`;
            document.body.appendChild(el);
        }

        // Close dropdown when clicking outside
        document.addEventListener('click', (e) => {
            if (!e.target.closest('#notifDropdown') && !e.target.closest('#notifBtn')) {
                Notifications.close();
            }
        });
    },

    toggle() {
        this._open ? this.close() : this.open();
    },

    open() {
        this._open = true;
        const btn      = document.getElementById('notifBtn');
        const dropdown = document.getElementById('notifDropdown');
        if (!btn || !dropdown) return;

        // Position dropdown below the bell
        const rect = btn.getBoundingClientRect();
        const isRtl = document.documentElement.getAttribute('dir') === 'rtl';
        dropdown.style.top = (rect.bottom + 8) + 'px';
        if (isRtl) {
            dropdown.style.left  = rect.left + 'px';
            dropdown.style.right = 'auto';
        } else {
            dropdown.style.right = (window.innerWidth - rect.right) + 'px';
            dropdown.style.left  = 'auto';
        }
        dropdown.classList.add('open');
    },

    close() {
        this._open = false;
        const dropdown = document.getElementById('notifDropdown');
        if (dropdown) dropdown.classList.remove('open');
    },

    async refresh() {
        if (!Auth.isClient()) return;
        try {
            const data = await API.get('/api/notifications');
            this._items = data.items || [];
            this._render();
            this._updateBadge(data.total || 0, data.critical || 0);
        } catch (e) {
            // Silently fail — notifications are non-critical
        }
    },

    _updateBadge(total, critical) {
        const btn = document.getElementById('notifBtn');
        if (!btn) return;

        // Remove old badge span if exists
        const oldBadge = btn.querySelector('.notif-count-badge');
        if (oldBadge) oldBadge.remove();

        if (total > 0) {
            btn.classList.add('notif-dot');
            const badge = document.createElement('span');
            badge.className = 'notif-count-badge';
            badge.textContent = total > 9 ? '9+' : total;
            btn.appendChild(badge);
        } else {
            btn.classList.remove('notif-dot');
        }
    },

    _render() {
        const list = document.getElementById('notifList');
        const countEl = document.getElementById('notifCount');
        if (!list) return;

        if (this._items.length === 0) {
            list.innerHTML = '<div class="notif-empty"><i class="fas fa-check-circle"></i> All clear!</div>';
            if (countEl) countEl.style.display = 'none';
            return;
        }

        if (countEl) {
            countEl.textContent = this._items.length;
            countEl.style.display = 'inline-flex';
        }

        list.innerHTML = this._items.map(item => {
            const severityClass = {
                critical: 'notif-item-critical',
                warning:  'notif-item-warning',
                info:     'notif-item-info'
            }[item.severity] || 'notif-item-info';

            return `<a href="${item.link}" class="notif-item ${severityClass}" onclick="Notifications.close()">
                <div class="notif-item-icon"><i class="fas ${item.icon}"></i></div>
                <div class="notif-item-body">
                    <div class="notif-item-title">${item.title}</div>
                    <div class="notif-item-msg">${item.message}</div>
                </div>
                <i class="fas fa-chevron-right notif-item-arrow"></i>
            </a>`;
        }).join('');
    }
};

// ── PWA / SERVICE WORKER ─────────────────────────────────────
const PWA = {
    init() {
        if (!('serviceWorker' in navigator)) return;
        // Only register on pages that include idb-helper.js (client portal)
        if (typeof IDB === 'undefined') return;

        window.addEventListener('load', () => {
            navigator.serviceWorker.register('/service-worker.js')
                .then(reg => console.info('SW registered:', reg.scope))
                .catch(err => console.error('SW registration failed:', err));
        });

        window.addEventListener('offline', () => {
            if (typeof Toast !== 'undefined') {
                Toast.warning('You are offline. Sales will be queued safely.');
            }
        });
    }
};

// ── NUMBER TO WORDS (AED) ─────────────────────────────────────
function numberToWordsAED(amount) {
    const ones = ['','One','Two','Three','Four','Five','Six','Seven','Eight','Nine',
                  'Ten','Eleven','Twelve','Thirteen','Fourteen','Fifteen','Sixteen',
                  'Seventeen','Eighteen','Nineteen'];
    const tens = ['','','Twenty','Thirty','Forty','Fifty','Sixty','Seventy','Eighty','Ninety'];

    function _words(n) {
        if (n === 0) return '';
        if (n < 20)  return ones[n];
        if (n < 100) return tens[Math.floor(n / 10)] + (n % 10 ? ' ' + ones[n % 10] : '');
        if (n < 1000)
            return ones[Math.floor(n / 100)] + ' Hundred'
                + (n % 100 ? ' ' + _words(n % 100) : '');
        if (n < 1000000)
            return _words(Math.floor(n / 1000)) + ' Thousand'
                + (n % 1000 ? ' ' + _words(n % 1000) : '');
        if (n < 1000000000)
            return _words(Math.floor(n / 1000000)) + ' Million'
                + (n % 1000000 ? ' ' + _words(n % 1000000) : '');
        return _words(Math.floor(n / 1000000000)) + ' Billion'
            + (n % 1000000000 ? ' ' + _words(n % 1000000000) : '');
    }

    const abs = Math.abs(parseFloat(amount) || 0);
    const dirhams = Math.floor(abs);
    const fils = Math.round((abs - dirhams) * 100);

    let result = _words(dirhams) || 'Zero';
    result += ' Dirhams';
    if (fils > 0) result += ' And ' + _words(fils) + ' Fils';
    result += ' Only';
    if (parseFloat(amount) < 0) result = 'Minus ' + result;
    return result;
}

// ── TAX INVOICE PRINT ─────────────────────────────────────────
/**
 * Opens a new window with a formatted Tax Invoice (Fixed) and triggers print.
 *
 * @param {Object} data        - Transaction result (from SaleService or PurchaseService)
 * @param {string} voucherType - 'SAL' | 'PUR'
 * @param {Array}  cartItems   - Line items (cart array from sales.html OR state.items from purchase.html)
 */
async function printTaxInvoice(data, voucherType, cartItems) {
    if (!data) { if (typeof Toast !== 'undefined') Toast.warning('No transaction data available.'); return; }

    const bizId      = Auth.getBusinessId();
    const bizName    = Auth.getBusinessName();
    const branchName = Auth.getBranchName() || '';
    const isSale     = voucherType === 'SAL';

    // ── Resolve party info ─────────────────────────────────
    const partyName  = isSale ? (data.customerName || '') : (data.supplierName || '');
    const partyId    = isSale ? data.customerId : data.supplierId;
    const voucherNo  = isSale ? data.receiptNumber : data.invoiceNumber;
    const dateStr    = isSale
        ? (data.createdAt ? data.createdAt.substring(0, 10) : new Date().toISOString().substring(0, 10))
        : (data.purchaseDate ? data.purchaseDate.substring(0, 10) : new Date().toISOString().substring(0, 10));
    const payMethod  = data.paymentMethod || '—';
    const cashier    = isSale ? (data.cashierName || '') : (data.createdBy || '');
    const inclDecl   = isSale && (data.includeReverseChargeDeclaration === true);

    // ── Amounts ────────────────────────────────────────────
    const subtotal    = parseFloat(data.subtotal   || 0);
    const discount    = parseFloat(data.discountAmount || 0);
    const premium     = parseFloat(data.premiumAmount  || 0);
    const vat         = parseFloat(data.vatAmount   || 0);
    const roundOff    = parseFloat(data.roundOffAmount || 0);
    const total       = parseFloat(data.totalAmount || 0);

    // ── Fetch ledger narration (best-effort) ───────────────
    let ledgerNarration = '';
    if (partyId && bizId) {
        try {
            const stmt = await fetch(
                `/api/party-ledger/${partyId}/statement?businessId=${bizId}`
            ).then(r => r.ok ? r.json() : null);
            if (stmt && stmt.entries) {
                const match = stmt.entries.find(e => e.voucherNumber === voucherNo);
                if (match) ledgerNarration = match.narration || '';
            }
        } catch (_) { /* non-critical */ }
    }

    // ── Format helpers ─────────────────────────────────────
    const fmtAed = v => 'AED ' + Number(v || 0).toLocaleString('en-AE', { minimumFractionDigits: 2, maximumFractionDigits: 2 });
    const fmtDate = s => {
        if (!s) return '';
        const d = new Date(s);
        return isNaN(d) ? s : d.toLocaleDateString('en-GB', { day: '2-digit', month: 'short', year: 'numeric' });
    };
    const esc = s => String(s || '').replace(/&/g,'&amp;').replace(/</g,'&lt;').replace(/>/g,'&gt;').replace(/"/g,'&quot;');

    // ── Line items ─────────────────────────────────────────
    let itemRows = '';
    const items = Array.isArray(cartItems) ? cartItems : [];
    items.forEach((item, idx) => {
        const name  = esc(item.productName || item.name || 'Item');
        const purity = esc(item.purity || item.scrapPurity || '—');
        const unit  = esc(item.unitType || item.unit || 'PCS');
        const qty   = parseFloat(item.quantity || item.qty || item.quantitySold || 0);
        const rate  = parseFloat(item.unitPrice || item.pricePerGram || item.priceAtSale || item.price || 0);
        const lineTotal = parseFloat(item.totalPrice || item.totalAmount || (qty * rate) || 0);
        itemRows += `
        <tr>
            <td style="text-align:center">${idx + 1}</td>
            <td>${name}</td>
            <td style="text-align:center">${purity}</td>
            <td style="text-align:center">${unit}</td>
            <td style="text-align:right">${qty % 1 === 0 ? qty : qty.toFixed(3)}</td>
            <td style="text-align:right">${rate.toFixed(2)}</td>
            <td style="text-align:right">${lineTotal.toFixed(2)}</td>
        </tr>`;
    });

    // ── CR/DR direction ────────────────────────────────────
    const direction = isSale ? 'DEBITED' : 'CREDITED';

    // ── Declaration note ───────────────────────────────────
    const declarationHtml = inclDecl ? `
        <div style="margin-top:20px;padding:10px 14px;border:1px solid #666;border-radius:4px;font-size:8.5pt;color:#333;line-height:1.55">
            <strong style="display:block;margin-bottom:4px">UAE Reverse-Charge VAT Declaration (Cabinet Decision No. 127/2024)</strong>
            The supplier named herein is not registered for VAT in the UAE. Pursuant to Cabinet Decision
            No. 127/2024, the recipient (buyer) is responsible for self-accounting for VAT on this
            supply under the reverse-charge mechanism. The applicable VAT must be declared and paid
            directly to the Federal Tax Authority by the recipient.
        </div>` : '';

    // ── Totals section rows ────────────────────────────────
    let totalsHtml = `
        <tr><td colspan="5" style="text-align:right;font-weight:600">Subtotal</td>
            <td style="text-align:right">${subtotal.toFixed(2)}</td></tr>`;
    if (discount > 0) totalsHtml += `
        <tr><td colspan="5" style="text-align:right;color:#dc2626">Discount</td>
            <td style="text-align:right;color:#dc2626">- ${discount.toFixed(2)}</td></tr>`;
    if (premium !== 0) totalsHtml += `
        <tr><td colspan="5" style="text-align:right;color:#059669">Premium</td>
            <td style="text-align:right;color:#059669">${premium >= 0 ? '+ ' : ''}${premium.toFixed(2)}</td></tr>`;
    if (vat > 0) totalsHtml += `
        <tr><td colspan="5" style="text-align:right">VAT (${data.vatPercent || 5}%)</td>
            <td style="text-align:right">${vat.toFixed(2)}</td></tr>`;
    if (roundOff !== 0) totalsHtml += `
        <tr><td colspan="5" style="text-align:right;color:#6b7280">Round-off</td>
            <td style="text-align:right;color:#6b7280">${roundOff >= 0 ? '+ ' : ''}${roundOff.toFixed(2)}</td></tr>`;
    totalsHtml += `
        <tr style="background:#f0f0f0;font-weight:700">
            <td colspan="5" style="text-align:right;border-top:2px solid #333;font-size:11pt">TOTAL</td>
            <td style="text-align:right;border-top:2px solid #333;font-size:11pt">${total.toFixed(2)}</td>
        </tr>`;

    // ── Build HTML ─────────────────────────────────────────
    const html = `<!DOCTYPE html>
<html lang="en">
<head>
<meta charset="UTF-8"/>
<title>Tax Invoice – ${esc(voucherNo)}</title>
<style>
    * { box-sizing:border-box; margin:0; padding:0 }
    body { font-family:'Arial',sans-serif; font-size:9.5pt; color:#111; padding:18mm 16mm; }
    h1 { font-size:16pt; font-weight:700; text-align:center; letter-spacing:.12em; text-transform:uppercase; margin:6px 0 2px }
    .double-rule { border:none; border-top:3px double #000; margin:4px 0 }
    .section-title { font-size:8pt; text-align:center; letter-spacing:.1em; text-transform:uppercase; color:#555; margin-bottom:10px }
    .header-biz { text-align:center; margin-bottom:12px }
    .biz-name { font-size:13pt; font-weight:700; letter-spacing:.04em }
    .biz-sub  { font-size:8.5pt; color:#555 }
    table.items { width:100%; border-collapse:collapse; font-size:9pt; margin:10px 0 }
    table.items th { background:#222; color:#fff; padding:5px 7px; font-size:8pt; text-align:left; }
    table.items td { padding:4px 7px; border-bottom:1px solid #ddd; }
    table.items tfoot td { background:#f5f5f5; }
    .info-grid { display:flex; gap:20px; margin:10px 0 }
    .info-box { flex:1; border:1px solid #bbb; border-radius:3px; padding:8px 10px; font-size:8.5pt; line-height:1.6 }
    .info-box dt { font-size:7.5pt; font-weight:700; color:#777; text-transform:uppercase; letter-spacing:.04em }
    .info-box dd { font-weight:600; color:#111; margin-bottom:2px }
    .acct-update { margin:14px 0 10px; padding:8px 12px; background:#f0f0f0; border:1px solid #ccc; border-radius:3px; font-size:9pt }
    .narration { margin:8px 0; font-size:8.5pt; color:#444; font-style:italic }
    .sig-row { display:flex; gap:30px; margin-top:30px }
    .sig-box { flex:1; border-top:1px solid #333; padding-top:4px; text-align:center; font-size:8pt; color:#555 }
    .words-line { font-size:8pt; color:#333; margin:4px 0 10px; font-style:italic }
    @media print { body { padding:10mm 12mm } button { display:none } }
</style>
</head>
<body>

<!-- Company header -->
<div class="header-biz">
    <div class="biz-name">${esc(bizName)}</div>
    ${branchName ? `<div class="biz-sub">${esc(branchName)}</div>` : ''}
</div>

<hr class="double-rule">
<h1>Tax Invoice (Fixed)</h1>
<hr class="double-rule">
<div class="section-title">Accounts Copy</div>

<!-- Party info + voucher info -->
<div class="info-grid">
    <div class="info-box">
        <dl>
            <dt>Party</dt><dd>${esc(partyName || '—')}</dd>
            <dt>Type</dt><dd>${isSale ? 'Customer' : 'Supplier'}</dd>
        </dl>
    </div>
    <div class="info-box" style="text-align:right">
        <dl>
            <dt>Voucher No.</dt><dd>${esc(voucherNo || '—')}</dd>
            <dt>Date</dt><dd>${fmtDate(dateStr)}</dd>
            <dt>Branch</dt><dd>${esc(branchName || '—')}</dd>
            <dt>Ref. By</dt><dd>${esc(cashier || '—')}</dd>
            <dt>Payment</dt><dd>${esc(payMethod)}</dd>
        </dl>
    </div>
</div>

<!-- Line items table -->
<table class="items">
    <thead>
        <tr>
            <th style="width:30px;text-align:center">#</th>
            <th>Description</th>
            <th style="width:70px;text-align:center">Purity</th>
            <th style="width:50px;text-align:center">Unit</th>
            <th style="width:65px;text-align:right">Qty</th>
            <th style="width:80px;text-align:right">Rate (AED)</th>
            <th style="width:90px;text-align:right">Amount (AED)</th>
        </tr>
    </thead>
    <tbody>${itemRows || '<tr><td colspan="7" style="text-align:center;color:#999">No items</td></tr>'}</tbody>
    <tfoot>${totalsHtml}</tfoot>
</table>

<!-- Amount in words -->
<div class="words-line">Amount in Words: <strong>${numberToWordsAED(total)}</strong></div>

<!-- Account update line -->
<div class="acct-update">
    <strong>Account Update:</strong>
    ${esc(partyName || 'Party')} account has been
    <strong>${direction}</strong> with
    <strong>${fmtAed(total)}</strong>
    against ${isSale ? 'Sale' : 'Purchase'} Voucher No. <strong>${esc(voucherNo || '')}</strong>.
</div>

<!-- Narration footer -->
${ledgerNarration ? `<div class="narration"><strong>Narration:</strong> ${esc(ledgerNarration)}</div>` : ''}

${declarationHtml}

<!-- Signature blocks -->
<div class="sig-row">
    <div class="sig-box">Prepared By</div>
    <div class="sig-box">Checked By</div>
    <div class="sig-box">Authorised Signatory</div>
    <div class="sig-box">${esc(partyName || 'Party')} Signature</div>
</div>

<script>window.onload = () => window.print();<\/script>
</body>
</html>`;

    const win = window.open('', '_blank', 'width=900,height=700');
    if (!win) { if (typeof Toast !== 'undefined') Toast.error('Pop-up blocked — please allow pop-ups for this page.'); return; }
    win.document.write(html);
    win.document.close();
}

// ── INIT ON DOM READY ────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    Sidebar.init();
    SyncManager.init();
    PWA.init();
    ProfileModal.init();
    Notifications.init();
    initPasswordToggles();

    if (Auth.isClient() && Auth.isOwnerGlobal()
        && !BranchGuard.isCurrentPageGlobalSafe()) {
        BranchGuard.require();
    }
});