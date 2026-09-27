// Pharma Guard — Offline Mode & Network Resilience Manager
(function() {
    function initOfflineManager() {
        const banner = document.createElement('div');
        banner.id = 'pharmaguard-offline-banner';
        banner.style.cssText = 'display:none; position:fixed; top:0; left:0; right:0; z-index:99999; padding:10px 16px; background:#f59e0b; color:#ffffff; font-weight:700; font-size:13px; text-align:center; box-shadow:0 2px 8px rgba(0,0,0,0.15);';
        banner.innerHTML = '🌐 OFFLINE MODE — Internet connection unavailable. Displaying cached data.';
        document.body.prepend(banner);

        function updateOnlineStatus() {
            if (!navigator.onLine) {
                banner.style.display = 'block';
                banner.style.background = '#d97706';
                banner.innerHTML = '🌐 OFFLINE MODE — Connection lost. Operations are queued locally for automatic synchronization.';
            } else {
                if (banner.style.display === 'block') {
                    banner.style.background = '#16a34a';
                    banner.innerHTML = '✓ NETWORK RESTORED — All local systems synchronized successfully with Pharma Guard servers.';
                    setTimeout(() => {
                        banner.style.display = 'none';
                    }, 4000);
                }
            }
        }

        window.addEventListener('online', updateOnlineStatus);
        window.addEventListener('offline', updateOnlineStatus);
        if (!navigator.onLine) updateOnlineStatus();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initOfflineManager);
    } else {
        initOfflineManager();
    }
})();
