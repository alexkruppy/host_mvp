const API_BASE = 'http://localhost:8080';

// ========== Toast Notifications ==========
function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    if (!container) return;
    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    const icons = { success: '✓', error: '✕', info: 'ℹ' };
    toast.innerHTML = `<span>${icons[type] || 'ℹ'}</span> ${message}`;
    container.appendChild(toast);
    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(40px)';
        toast.style.transition = '0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 3500);
}

// ========== Token & User ==========
function getToken() {
    return localStorage.getItem('token');
}

function getUser() {
    try {
        const data = localStorage.getItem('user');
        return data ? JSON.parse(data) : null;
    } catch { return null; }
}

function updateAuthUI() {
    const token = getToken();
    const user = getUser();
    const navAuth = document.getElementById('navAuth');
    const navUser = document.getElementById('navUser');
    const userName = document.getElementById('userName');

    if (token && user) {
        if (navAuth) navAuth.style.display = 'none';
        if (navUser) {
            navUser.style.display = 'flex';
            if (userName) userName.textContent = user.fullName || user.email;
        }
    } else {
        if (navAuth) navAuth.style.display = 'flex';
        if (navUser) navUser.style.display = 'none';
    }
}

function logout() {
    localStorage.removeItem('token');
    localStorage.removeItem('user');
    showToast('Logged out successfully', 'info');
    setTimeout(() => { window.location.href = '/'; }, 500);
}

// ========== API Request ==========
async function apiRequest(method, path, body, isFormData) {
    const url = API_BASE + path;
    const headers = {};
    const token = getToken();

    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }

    if (!isFormData) {
        headers['Content-Type'] = 'application/json';
    }

    const options = { method, headers };
    if (body) {
        options.body = isFormData ? body : JSON.stringify(body);
    }

    let response;
    try {
        response = await fetch(url, options);
    } catch (e) {
        throw new Error('Network error — check your connection');
    }

    if (!response.ok) {
        let errMsg;
        try {
            const err = await response.json();
            errMsg = err.error || err.message || 'Request failed';
        } catch {
            errMsg = `Error ${response.status}`;
        }
        throw new Error(errMsg);
    }
    return response;
}

// ========== Auth ==========
async function handleLogin(event) {
    event.preventDefault();
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const errorEl = document.getElementById('loginError');

    try {
        const res = await apiRequest('POST', '/api/auth/login', { email, password });
        const data = await res.json();
        localStorage.setItem('token', data.token);
        localStorage.setItem('user', JSON.stringify({
            email: data.email,
            fullName: data.fullName,
            role: data.role,
            premiumActive: data.premiumActive
        }));
        showToast('Welcome back, ' + data.fullName + '!', 'success');
        setTimeout(() => { window.location.href = '/dashboard'; }, 500);
    } catch (e) {
        errorEl.style.display = 'block';
        errorEl.textContent = '✕ ' + e.message;
    }
}

async function handleRegister(event) {
    event.preventDefault();
    const fullName = document.getElementById('fullName').value;
    const email = document.getElementById('email').value;
    const password = document.getElementById('password').value;
    const errorEl = document.getElementById('registerError');

    try {
        const res = await apiRequest('POST', '/api/auth/register', { email, password, fullName });
        const data = await res.json();
        localStorage.setItem('token', data.token);
        localStorage.setItem('user', JSON.stringify({
            email: data.email,
            fullName: data.fullName,
            role: data.role,
            premiumActive: data.premiumActive
        }));
        showToast('Account created! Welcome, ' + data.fullName, 'success');
        setTimeout(() => { window.location.href = '/dashboard'; }, 500);
    } catch (e) {
        errorEl.style.display = 'block';
        errorEl.textContent = '✕ ' + e.message;
    }
}

// ========== Videos ==========
async function loadVideos(filter) {
    const grid = document.getElementById('videoGrid');
    if (!grid) return;

    grid.innerHTML = `
        <div class="skeleton-grid" id="skeletonGrid">
            ${Array(6).fill(0).map(() => `
                <div class="skeleton-card">
                    <div class="skeleton-thumb"></div>
                    <div class="skeleton-body">
                        <div class="skeleton-line"></div>
                        <div class="skeleton-line"></div>
                    </div>
                </div>
            `).join('')}
        </div>
    `;

    try {
        let path = '/api/videos';
        if (filter === 'free') path = '/api/videos/free';
        else if (filter === 'premium') path = '/api/videos/premium';

        const res = await apiRequest('GET', path);
        const videos = await res.json();
        renderVideoGrid(grid, videos);
    } catch (e) {
        grid.innerHTML = `
            <div class="loading">
                <p style="color:var(--error);font-size:1.1rem">Failed to load videos</p>
                <p style="color:var(--text-muted);margin-top:8px">${e.message}</p>
            </div>
        `;
    }
}

async function loadFreeVideos() {
    const grid = document.getElementById('videoGrid');
    if (!grid) return;

    try {
        const res = await apiRequest('GET', '/api/videos/free');
        const videos = await res.json();
        renderVideoGrid(grid, videos.slice(0, 6));
    } catch (e) {
        grid.innerHTML = '<div class="loading"><p style="color:var(--text-muted)">No videos available yet</p></div>';
    }
}

function renderVideoGrid(grid, videos) {
    if (!videos || videos.length === 0) {
        grid.innerHTML = `
            <div class="loading">
                <p style="font-size:2rem;margin-bottom:12px">🎬</p>
                <p style="color:var(--text-muted)">No videos available</p>
            </div>
        `;
        return;
    }

    grid.innerHTML = videos.map(v => `
        <div class="video-card" onclick="window.location.href='/watch/${v.id}'">
            <div class="video-thumb">
                🎬
                <div class="video-play-overlay">
                    <div class="play-icon">▶</div>
                </div>
                <span class="video-badge ${v.premium ? 'badge-premium' : 'badge-free'}">
                    ${v.premium ? 'Premium' : 'Free'}
                </span>
            </div>
            <div class="video-card-body">
                <div class="video-card-title">${escapeHtml(v.title)}</div>
                <div class="video-card-meta">
                    <span class="video-card-author">${escapeHtml(v.uploadedBy || 'Unknown')}</span>
                    <span class="video-card-views">${v.views || 0} views</span>
                </div>
            </div>
        </div>
    `).join('');
}

// ========== Single Video ==========
async function loadVideo(id) {
    const container = document.getElementById('watchContainer');
    if (!container) return;

    try {
        const res = await apiRequest('GET', `/api/videos/${id}`);
        const video = await res.json();

        const token = getToken();
        const user = getUser();
        let canWatch = true;

        if (video.premium) {
            if (!token || !user || !user.premiumActive) {
                canWatch = false;
            }
        }

        if (!canWatch) {
            container.innerHTML = `
                <div class="watch-premium-lock">
                    <div class="watch-premium-lock-icon">💎</div>
                    <h2>Premium Content</h2>
                    <p>This video requires an active premium subscription.</p>
                    <a href="/premium" class="btn btn-premium btn-lg">Upgrade to Premium</a>
                    ${!token ? '<p style="margin-top:16px;color:var(--text-muted)"><a href="/auth/login">Sign in</a> to your account first</p>' : ''}
                </div>
            `;
            return;
        }

        let videoUrl = `/api/stream/video/${id}`;
        let hlsSource = '';

        if (video.hlsReady && video.hlsPath) {
            hlsSource = `/api/stream/hls/${id}/index.m3u8`;
        }

        apiRequest('POST', `/api/videos/${id}/views`).catch(() => {});

        const formattedDate = video.uploadedAt ? new Date(video.uploadedAt).toLocaleDateString('en-US', {
            year: 'numeric', month: 'short', day: 'numeric'
        }) : '';

        container.innerHTML = `
            <div class="watch-player-wrapper">
                <video id="videoPlayer" class="video-js vjs-big-play-centered" controls preload="auto" data-setup='{"fluid":true,"controls":true,"autoplay":false}'>
                    <source src="${hlsSource || videoUrl}" type="${hlsSource ? 'application/x-mpegURL' : (video.contentType || 'video/mp4')}">
                </video>
            </div>
            <div class="watch-details">
                <h1 class="watch-title">${escapeHtml(video.title)}</h1>
                <div class="watch-meta">
                    <span class="watch-meta-item">👤 ${escapeHtml(video.uploadedBy || 'Unknown')}</span>
                    <span class="watch-meta-divider"></span>
                    <span class="watch-meta-item">👁 ${video.views || 0} views</span>
                    ${video.duration ? `<span class="watch-meta-divider"></span><span class="watch-meta-item">⏱ ${formatDuration(video.duration)}</span>` : ''}
                    ${formattedDate ? `<span class="watch-meta-divider"></span><span class="watch-meta-item">📅 ${formattedDate}</span>` : ''}
                    ${video.premium ? `<span class="watch-meta-divider"></span><span class="video-badge badge-premium" style="position:static;font-size:0.75rem">Premium</span>` : ''}
                </div>
                ${video.description ? `<div class="watch-description">${escapeHtml(video.description)}</div>` : ''}
            </div>
        `;

        if (typeof videojs !== 'undefined') {
            const player = videojs('videoPlayer', {
                html5: { hls: { overrideNative: true } }
            });
            player.ready(() => {
                showToast('Press play to start watching', 'info');
            });
        }

    } catch (e) {
        container.innerHTML = `
            <div class="loading">
                <p style="color:var(--error);font-size:1.1rem">Failed to load video</p>
                <p style="color:var(--text-muted);margin-top:8px">${escapeHtml(e.message)}</p>
            </div>
        `;
    }
}

function formatDuration(seconds) {
    const m = Math.floor(seconds / 60);
    const s = Math.floor(seconds % 60);
    if (m >= 60) {
        const h = Math.floor(m / 60);
        return `${h}h ${m % 60}m`;
    }
    return `${m}m ${s}s`;
}

// ========== Upload ==========
function onFileSelect(event) {
    const file = event.target.files[0];
    if (!file) return;
    const display = document.getElementById('fileNameDisplay');
    const text = document.getElementById('fileNameText');
    const zone = document.getElementById('fileDropZone');
    if (display && text) {
        text.textContent = `📄 ${file.name} (${(file.size / (1024*1024)).toFixed(1)} MB)`;
        display.classList.add('show');
        if (zone) zone.style.display = 'none';
    }
}

function clearFile() {
    const input = document.getElementById('file');
    if (input) input.value = '';
    const display = document.getElementById('fileNameDisplay');
    const zone = document.getElementById('fileDropZone');
    if (display) display.classList.remove('show');
    if (zone) zone.style.display = 'block';
}

async function handleUpload(event) {
    event.preventDefault();
    const title = document.getElementById('title').value;
    const description = document.getElementById('description').value;
    const file = document.getElementById('file').files[0];
    const isPremium = document.getElementById('premium').checked;
    const errorEl = document.getElementById('uploadError');
    const progress = document.getElementById('uploadProgress');
    const progressFill = document.getElementById('progressFill');
    const progressText = document.getElementById('progressText');
    const uploadBtn = document.getElementById('uploadBtn');

    if (!file) {
        errorEl.style.display = 'block';
        errorEl.textContent = '✕ Please select a file';
        return;
    }

    if (file.size > 2 * 1024 * 1024 * 1024) {
        errorEl.style.display = 'block';
        errorEl.textContent = '✕ File exceeds 2GB limit';
        return;
    }

    errorEl.style.display = 'none';
    progress.style.display = 'block';
    uploadBtn.disabled = true;
    uploadBtn.textContent = 'Uploading...';

    const formData = new FormData();
    formData.append('file', file);
    formData.append('title', title);
    formData.append('description', description);
    formData.append('premium', isPremium);

    try {
        const token = getToken();
        await new Promise((resolve, reject) => {
            const xhr = new XMLHttpRequest();
            xhr.upload.onprogress = (e) => {
                if (e.lengthComputable) {
                    const pct = Math.round((e.loaded / e.total) * 100);
                    progressFill.style.width = pct + '%';
                    progressText.textContent = `Uploading... ${pct}%`;
                }
            };
            xhr.open('POST', API_BASE + '/api/videos/upload');
            xhr.setRequestHeader('Authorization', 'Bearer ' + token);
            xhr.onload = () => {
                if (xhr.status >= 200 && xhr.status < 300) resolve();
                else {
                    try {
                        const err = JSON.parse(xhr.responseText);
                        reject(new Error(err.error || 'Upload failed'));
                    } catch {
                        reject(new Error('Upload failed (' + xhr.status + ')'));
                    }
                }
            };
            xhr.onerror = () => reject(new Error('Network error during upload'));
            xhr.send(formData);
        });

        progressFill.style.width = '100%';
        progressText.textContent = '✅ Upload complete! Processing video...';
        showToast('Video uploaded successfully! Processing HLS...', 'success');
        setTimeout(() => { window.location.href = '/dashboard'; }, 2000);
    } catch (e) {
        progress.style.display = 'none';
        uploadBtn.disabled = false;
        uploadBtn.textContent = 'Upload Video';
        errorEl.style.display = 'block';
        errorEl.textContent = '✕ ' + e.message;
    }
}

// ========== Stripe ==========
async function createCheckoutSession() {
    try {
        const res = await apiRequest('POST', '/api/payments/create-checkout-session');
        const data = await res.json();
        if (data.url) {
            window.location.href = data.url;
        }
    } catch (e) {
        showToast(e.message, 'error');
    }
}

// ========== UI Helpers ==========
function setActiveTab(filter) {
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    const tab = document.querySelector(`[onclick*="loadVideos('${filter}'"]`) ||
                document.querySelector(`[onclick*='loadVideos("${filter}"']`);
    if (!tab) {
        document.querySelectorAll('.tab').forEach(t => {
            if (t.textContent.toLowerCase() === filter) t.classList.add('active');
        });
    } else {
        tab.classList.add('active');
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ========== File Drop Zone ==========
document.addEventListener('DOMContentLoaded', () => {
    const zone = document.getElementById('fileDropZone');
    if (zone) {
        zone.addEventListener('dragover', (e) => {
            e.preventDefault();
            zone.classList.add('dragover');
        });
        zone.addEventListener('dragleave', () => {
            zone.classList.remove('dragover');
        });
        zone.addEventListener('drop', (e) => {
            e.preventDefault();
            zone.classList.remove('dragover');
            const files = e.dataTransfer.files;
            if (files.length) {
                document.getElementById('file').files = files;
                onFileSelect({ target: { files: [files[0]] } });
            }
        });
        zone.addEventListener('click', () => {
            document.getElementById('file').click();
        });
    }
});
