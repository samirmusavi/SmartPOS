// Sidebar dropdown logic — shared across all pages.
// Included in every page that has the standard sidebar.

function toggleSalesMenu() {
    var children = document.getElementById('salesMenuChildren');
    var chevron  = document.getElementById('salesMenuChevron');
    var toggle   = document.getElementById('salesMenuToggle');
    if (!children) return;
    var open = children.classList.toggle('open');
    if (chevron) chevron.style.transform = open ? 'rotate(180deg)' : '';
}

// Auto-expand the sales submenu when the user is on a sales page,
// and mark the correct child link as active.
(function () {
    var path = window.location.pathname;
    var salesPages = ['/sales.html', '/sales-list.html', '/sales-return.html'];
    if (salesPages.indexOf(path) !== -1) {
        var children = document.getElementById('salesMenuChildren');
        var chevron  = document.getElementById('salesMenuChevron');
        var toggle   = document.getElementById('salesMenuToggle');
        if (children) {
            children.classList.add('open');
            var links = children.querySelectorAll('.nav-child');
            links.forEach(function (a) {
                if (a.getAttribute('href') === path) a.classList.add('active');
            });
        }
        if (chevron) chevron.style.transform = 'rotate(180deg)';
        if (toggle)  toggle.classList.add('active-parent');
    }
})();
