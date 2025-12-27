// Minecraft 啟動器 JavaScript

class MinecraftLauncher {
    constructor() {
        this.isLoggedIn = false;
        this.selectedProfile = '生存模式';
        this.gameRunning = false;
        this.init();
    }

    init() {
        this.bindEvents();
        this.createFloatingElements();
        this.updateAnnouncements();
        this.checkForUpdates();
    }

    bindEvents() {
        // 登入按鈕事件
        const loginBtn = document.querySelector('.login-btn');
        loginBtn.addEventListener('click', () => this.handleLogin());

        // 開始遊戲按鈕事件
        const startGameBtn = document.querySelector('.start-game-btn');
        startGameBtn.addEventListener('click', () => this.handleStartGame());

        // 伺服器選擇事件
        const serverDropdown = document.querySelector('.server-dropdown');
        serverDropdown.addEventListener('change', (e) => this.handleServerChange(e));

        // 設定按鈕事件
        const settingsBtn = document.querySelector('.settings-btn');
        settingsBtn.addEventListener('click', () => this.openSettings());

        // Discord按鈕事件
        const discordBtn = document.querySelector('.discord-btn');
        discordBtn.addEventListener('click', () => this.openDiscord());
    }

    handleLogin() {
        const loginBtn = document.querySelector('.login-btn');
        
        if (!this.isLoggedIn) {
            // 模擬登入過程
            loginBtn.textContent = '登入中...';
            loginBtn.disabled = true;
            
            setTimeout(() => {
                this.isLoggedIn = true;
                loginBtn.textContent = '玩家123';
                loginBtn.style.background = 'linear-gradient(45deg, #4CAF50, #8BC34A)';
                loginBtn.disabled = false;
                this.showNotification('登入成功！歡迎回來！', 'success');
            }, 1500);
        } else {
            // 登出
            this.isLoggedIn = false;
            loginBtn.textContent = '登入';
            loginBtn.style.background = 'linear-gradient(45deg, #FF6B9D, #FF8E9B)';
            this.showNotification('已登出', 'info');
        }
    }

    handleStartGame() {
        if (!this.isLoggedIn) {
            this.showNotification('請先登入才能開始遊戲！', 'warning');
            return;
        }

        if (this.gameRunning) {
            this.showNotification('遊戲已在運行中！', 'info');
            return;
        }

        const startBtn = document.querySelector('.start-game-btn');
        const versionInfo = document.querySelector('.version-info');
        
        // 模擬遊戲啟動過程
        startBtn.textContent = '啟動中...';
        startBtn.disabled = true;
        versionInfo.textContent = '正在檢查遊戲文件...';
        
        setTimeout(() => {
            versionInfo.textContent = '正在啟動 Minecraft...';
        }, 1000);
        
        setTimeout(() => {
            this.gameRunning = true;
            startBtn.textContent = '遊戲運行中';
            startBtn.style.background = 'linear-gradient(45deg, #4CAF50, #8BC34A)';
            versionInfo.textContent = `${this.selectedProfile} - 運行中`;
            this.showNotification('遊戲啟動成功！', 'success');
            
            // 模擬遊戲結束
            setTimeout(() => {
                this.gameRunning = false;
                startBtn.textContent = '開始遊戲';
                startBtn.style.background = 'linear-gradient(45deg, #00BCD4, #2196F3)';
                startBtn.disabled = false;
                versionInfo.textContent = '準備開始';
                this.showNotification('遊戲已結束', 'info');
            }, 10000);
        }, 2000);
    }

    handleServerChange(event) {
        this.selectedProfile = event.target.value;
        if (this.selectedProfile !== '選擇配置 ▼') {
            this.showNotification(`已選擇配置: ${this.selectedProfile}`, 'info');
        }
    }

    openSettings() {
        this.showNotification('設定功能開發中...', 'info');
    }

    openDiscord() {
        this.showNotification('正在開啟 Discord...', 'info');
        // 在實際應用中，這裡會開啟Discord連結
    }

    showNotification(message, type = 'info') {
        // 創建通知元素
        const notification = document.createElement('div');
        notification.className = `notification ${type}`;
        notification.textContent = message;
        
        // 設定通知樣式
        Object.assign(notification.style, {
            position: 'fixed',
            top: '20px',
            right: '20px',
            padding: '12px 20px',
            borderRadius: '8px',
            color: 'white',
            fontWeight: '500',
            zIndex: '1000',
            transform: 'translateX(100%)',
            transition: 'transform 0.3s ease',
            maxWidth: '300px',
            wordWrap: 'break-word'
        });
        
        // 根據類型設定顏色
        const colors = {
            success: 'linear-gradient(45deg, #4CAF50, #8BC34A)',
            warning: 'linear-gradient(45deg, #FF9800, #FFC107)',
            error: 'linear-gradient(45deg, #F44336, #E91E63)',
            info: 'linear-gradient(45deg, #2196F3, #03A9F4)'
        };
        
        notification.style.background = colors[type] || colors.info;
        
        document.body.appendChild(notification);
        
        // 顯示動畫
        setTimeout(() => {
            notification.style.transform = 'translateX(0)';
        }, 100);
        
        // 自動移除
        setTimeout(() => {
            notification.style.transform = 'translateX(100%)';
            setTimeout(() => {
                if (notification.parentNode) {
                    notification.parentNode.removeChild(notification);
                }
            }, 300);
        }, 3000);
    }

    createFloatingElements() {
        const scene = document.querySelector('.character-scene');
        
        // 創建浮動的方塊元素
        for (let i = 0; i < 5; i++) {
            const block = document.createElement('div');
            block.className = 'floating-block';
            
            Object.assign(block.style, {
                position: 'absolute',
                width: '20px',
                height: '20px',
                background: `hsl(${Math.random() * 360}, 70%, 60%)`,
                borderRadius: '2px',
                opacity: '0.6',
                left: `${Math.random() * 80}%`,
                top: `${Math.random() * 80}%`,
                animation: `float ${3 + Math.random() * 2}s ease-in-out infinite`,
                animationDelay: `${Math.random() * 2}s`
            });
            
            scene.appendChild(block);
        }
    }

    updateAnnouncements() {
        const announcements = [
            '🎉 新的模組包已上線！快來體驗吧！',
            '🔧 伺服器維護將在今晚進行，預計1小時',
            '🎮 週末活動：建築比賽開始報名！',
            '📢 歡迎新玩家加入我們的社群！'
        ];
        
        const content = document.querySelector('.announcement-content');
        const randomAnnouncement = announcements[Math.floor(Math.random() * announcements.length)];
        
        setTimeout(() => {
            const newP = document.createElement('p');
            newP.textContent = randomAnnouncement;
            newP.style.color = '#2196F3';
            newP.style.fontWeight = '500';
            content.appendChild(newP);
        }, 2000);
    }

    checkForUpdates() {
        // 模擬檢查更新
        setTimeout(() => {
            const updateContent = document.querySelector('.update-content');
            const newUpdate = document.createElement('div');
            newUpdate.className = 'update-item';
            newUpdate.innerHTML = '<strong>v1.2.4 更新：</strong>優化了啟動器性能和界面體驗！';
            updateContent.insertBefore(newUpdate, updateContent.firstChild);
        }, 5000);
    }
}

// 當頁面載入完成時初始化啟動器
document.addEventListener('DOMContentLoaded', () => {
    const launcher = new MinecraftLauncher();
    
    // 添加一些額外的視覺效果
    const addParticles = () => {
        const container = document.querySelector('.launcher-container');
        
        for (let i = 0; i < 3; i++) {
            const particle = document.createElement('div');
            Object.assign(particle.style, {
                position: 'absolute',
                width: '4px',
                height: '4px',
                background: 'rgba(255,255,255,0.6)',
                borderRadius: '50%',
                left: `${Math.random() * 100}%`,
                top: `${Math.random() * 100}%`,
                animation: `float ${5 + Math.random() * 3}s ease-in-out infinite`,
                animationDelay: `${Math.random() * 3}s`,
                pointerEvents: 'none'
            });
            
            container.appendChild(particle);
            
            // 移除粒子
            setTimeout(() => {
                if (particle.parentNode) {
                    particle.parentNode.removeChild(particle);
                }
            }, 8000);
        }
    };
    
    // 定期添加粒子效果
    setInterval(addParticles, 3000);
    addParticles();
});

// 防止右鍵菜單（可選）
document.addEventListener('contextmenu', (e) => {
    e.preventDefault();
});

// 鍵盤快捷鍵
document.addEventListener('keydown', (e) => {
    if (e.key === 'F5') {
        e.preventDefault();
        location.reload();
    }
    
    if (e.key === 'F11') {
        e.preventDefault();
        if (document.fullscreenElement) {
            document.exitFullscreen();
        } else {
            document.documentElement.requestFullscreen();
        }
    }
});