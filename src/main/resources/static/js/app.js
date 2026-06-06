const API_BASE = 'http://localhost:8080';

function getToken() {
    return localStorage.getItem('token');
}

function getUser() {
    const data = localStorage.getItem('user');
    return data ? JSON.parse(data) : null;
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
    window.location.href = '/';
}

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

    const response = await fetch(url, options);
    if (!response.ok) {
        const err = await response.text();
        throw new Error(err || 'Request failed');
    }
    return response;
}

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
        window.location.href = '/dashboard';
    } catch (e) {
        errorEl.textContent = 'Invalid email or password';
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
        window.location.href = '/dashboard';
    } catch (e) {
        errorEl.textContent = 'Registration failed. Email may already be in use.';
    }
}

async function loadVideos(filter) {
    const grid = document.getElementById('videoGrid');
    grid.innerHTML = '<div class="loading">Loading videos...</div>';

    try {
        let path = '/api/videos';
        if (filter === 'free') path = '/api/videos/free';
        else if (filter === 'premium') path = '/api/videos/premium';

        const res = await apiRequest('GET', path);
        const videos = await res.json();
        renderVideoGrid(grid, videos);
    } catch (e) {
        grid.innerHTML = '<div class="loading">Failed to load videos</div>';
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
        grid.innerHTML = '<div class="loading">Failed to load videos</div>';
    }
}

function renderVideoGrid(grid, videos) {
    if (!videos || videos.length === 0) {
        grid.innerHTML = '<div class="loading">No videos available</div>';
        return;
    }

    grid.innerHTML = videos.map(v => `
        <div class="video-card" onclick="window.location.href='/watch/${v.id}'">
            <div class="video-thumb">🎬</div>
            <div class="video-info">
                <h3>${escapeHtml(v.title)}</h3>
                <div class="video-meta">
                    <span>${v.uploadedBy || 'Unknown'}</span>
                    <span class="video-badge ${v.premium ? 'badge-premium' : 'badge-free'}">
                        ${v.premium ? 'PREMIUM' : 'FREE'}
                    </span>
                </div>
            </div>
        </div>
    `).join('');
}

async function loadVideo(id) {
    const container = document.getElementById('watchContainer');
    if (!container) return;

    try {
        const res = await apiRequest('GET', `/api/videos/${id}`);
        const video = await res.json();

        const isPremium = video.premium;
        const user = getUser();
        const token = getToken();
        let canWatch = true;

        if (isPremium) {
            if (!token || !user || !user.premiumActive) {
                canWatch = false;
            }
        }

        if (!canWatch) {
            container.innerHTML = `
                <div class="auth-card" style="margin:0 auto;text-align:center">
                    <h2>Premium Content</h2>
                    <p style="margin:16px 0;color:#999">This video requires a premium subscription.</p>
                    <a href="/premium" class="btn btn-primary">Upgrade to Premium</a>
                </div>
            `;
            return;
        }

        let videoUrl = `/api/stream/video/${id}`;
        let hlsSource = '';

        if (video.hlsReady && video.hlsPath) {
            hlsSource = `/api/stream/hls/${id}/index.m3u8`;
        }

        // Increment views
        apiRequest('POST', `/api/videos/${id}/views`).catch(() => {});

        let playerHtml;
        if (hlsSource) {
            playerHtml = `
                <video id="videoPlayer" class="video-js vjs-big-play-centered vjs-16-9" controls preload="auto" data-setup='{"fluid":true}'>
                    <source src="${hlsSource}" type="application/x-mpegURL">
                    <p class="vjs-no-js">Video playback not supported</p>
                </video>
            `;
        } else {
            playerHtml = `
                <video id="videoPlayer" class="video-js vjs-big-play-centered vjs-16-9" controls preload="auto" data-setup='{"fluid":true}'>
                    <source src="${videoUrl}" type="${video.contentType || 'video/mp4'}">
                    <p class="vjs-no-js">Video playback not supported</p>
                </video>
            `;
        }

        container.innerHTML = `
            ${playerHtml}
            <div class="video-details">
                <h1>${escapeHtml(video.title)}</h1>
                <div class="meta">
                    Uploaded by ${escapeHtml(video.uploadedBy || 'Unknown')} |
                    ${video.views || 0} views |
                    ${video.duration ? Math.round(video.duration) + 's' : ''}
                    ${video.premium ? '| <span class="video-badge badge-premium">PREMIUM</span>' : ''}
                </div>
                ${video.description ? '<p>' + escapeHtml(video.description) + '</p>' : ''}
            </div>
        `;

        // Initialize Video.js
        if (typeof videojs !== 'undefined') {
            videojs('videoPlayer', {
                html5: {
                    hls: {
                        overrideNative: true
                    }
                }
            });
        }

    } catch (e) {
        container.innerHTML = '<div class="loading">Failed to load video</div>';
    }
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

    if (!file) {
        errorEl.textContent = 'Please select a file';
        return;
    }

    if (file.size > 2 * 1024 * 1024 * 1024) {
        errorEl.textContent = 'File exceeds 2GB limit';
        return;
    }

    errorEl.textContent = '';
    progress.style.display = 'block';

    const formData = new FormData();
    formData.append('file', file);
    formData.append('title', title);
    formData.append('description', description);
    formData.append('premium', isPremium);

    try {
        const token = getToken();
        const xhr = new XMLHttpRequest();

        xhr.upload.onprogress = (e) => {
            if (e.lengthComputable) {
                const pct = Math.round((e.loaded / e.total) * 100);
                progressFill.style.width = pct + '%';
                progressText.textContent = `Uploading... ${pct}%`;
            }
        };

        await new Promise((resolve, reject) => {
            xhr.open('POST', API_BASE + '/api/videos/upload');
            xhr.setRequestHeader('Authorization', 'Bearer ' + token);
            xhr.onload = () => {
                if (xhr.status >= 200 && xhr.status < 300) resolve();
                else reject(new Error(xhr.responseText));
            };
            xhr.onerror = () => reject(new Error('Upload failed'));
            xhr.send(formData);
        });

        progressText.textContent = 'Upload complete! Video is being processed.';
        setTimeout(() => { window.location.href = '/dashboard'; }, 2000);
    } catch (e) {
        progress.style.display = 'none';
        errorEl.textContent = 'Upload failed: ' + e.message;
    }
}

async function createCheckoutSession() {
    try {
        const res = await apiRequest('POST', '/api/payments/create-checkout-session');
        const data = await res.json();
        if (data.url) {
            window.location.href = data.url;
        }
    } catch (e) {
        alert('Failed to create checkout session. Please try again.');
    }
}

function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

function setActiveTab(filter) {
    document.querySelectorAll('.tab').forEach(t => t.classList.remove('active'));
    const tab = document.querySelector(`[onclick="loadVideos('${filter}')"]`);
    if (tab) tab.classList.add('active');
}
