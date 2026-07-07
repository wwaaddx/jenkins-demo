const API = '/api';

let currentSlug = null;
let allPosts = [];

const el = {
    list: document.getElementById('post-list'),
    search: document.getElementById('search'),
    title: document.getElementById('title'),
    categories: document.getElementById('categories'),
    tags: document.getElementById('tags'),
    content: document.getElementById('content'),
    preview: document.getElementById('preview'),
    previewArea: document.querySelector('.preview-area'),
    status: document.getElementById('status'),
    fileInput: document.getElementById('file-input'),
};

function showStatus(msg, type = 'success') {
    el.status.textContent = msg;
    el.status.className = 'status ' + type;
    setTimeout(() => {
        el.status.textContent = '';
        el.status.className = 'status';
    }, 4000);
}

function formatDate(dateStr) {
    if (!dateStr) return '';
    return dateStr.replace('T', ' ');
}

function renderList(posts) {
    el.list.innerHTML = '';
    posts.forEach(post => {
        const li = document.createElement('li');
        li.dataset.slug = post.slug;
        if (post.slug === currentSlug) li.classList.add('active');
        li.innerHTML = `
            <div class="title">${escapeHtml(post.title || post.slug)}</div>
            <div class="meta">${formatDate(post.date)}</div>
        `;
        li.addEventListener('click', () => selectPost(post.slug));
        el.list.appendChild(li);
    });
}

function escapeHtml(str) {
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

async function loadPosts() {
    try {
        const res = await fetch(`${API}/posts`);
        if (!res.ok) throw new Error('加载失败');
        allPosts = await res.json();
        filterList();
    } catch (err) {
        showStatus('文章列表加载失败：' + err.message, 'error');
    }
}

function filterList() {
    const kw = el.search.value.trim().toLowerCase();
    const filtered = kw
        ? allPosts.filter(p => (p.title || p.slug).toLowerCase().includes(kw))
        : allPosts;
    renderList(filtered);
}

async function selectPost(slug) {
    try {
        const res = await fetch(`${API}/posts/${encodeURIComponent(slug)}`);
        if (!res.ok) throw new Error('读取失败');
        const post = await res.json();
        currentSlug = post.slug;
        el.title.value = post.title || '';
        el.categories.value = (post.categories || []).join(', ');
        el.tags.value = (post.tags || []).join(', ');
        el.content.value = post.content || '';
        el.previewArea.classList.add('hidden');
        filterList();
    } catch (err) {
        showStatus('读取文章失败：' + err.message, 'error');
    }
}

function newPost() {
    currentSlug = null;
    el.title.value = '';
    el.categories.value = '';
    el.tags.value = '';
    el.content.value = '';
    el.previewArea.classList.add('hidden');
    filterList();
    el.title.focus();
}

function splitTags(value) {
    return value.split(/[,，]/)
        .map(s => s.trim())
        .filter(s => s.length > 0);
}

async function savePost() {
    const title = el.title.value.trim();
    if (!title) {
        showStatus('标题不能为空', 'error');
        return;
    }
    const payload = {
        title: title,
        categories: splitTags(el.categories.value),
        tags: splitTags(el.tags.value),
        content: el.content.value
    };
    try {
        let res;
        if (currentSlug) {
            res = await fetch(`${API}/posts/${encodeURIComponent(currentSlug)}`, {
                method: 'PUT',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
        } else {
            res = await fetch(`${API}/posts`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify(payload)
            });
        }
        if (!res.ok) {
            const err = await res.json();
            throw new Error(err.error || '保存失败');
        }
        const post = await res.json();
        currentSlug = post.slug;
        showStatus('保存成功');
        await loadPosts();
    } catch (err) {
        showStatus('保存失败：' + err.message, 'error');
    }
}

async function deletePost() {
    if (!currentSlug) return;
    if (!confirm('确定要删除这篇文章吗？')) return;
    try {
        const res = await fetch(`${API}/posts/${encodeURIComponent(currentSlug)}`, {
            method: 'DELETE'
        });
        if (!res.ok) throw new Error('删除失败');
        showStatus('删除成功');
        newPost();
        await loadPosts();
    } catch (err) {
        showStatus('删除失败：' + err.message, 'error');
    }
}

function togglePreview() {
    if (el.previewArea.classList.contains('hidden')) {
        el.preview.innerHTML = marked.parse(el.content.value || '（无内容）');
        el.previewArea.classList.remove('hidden');
    } else {
        el.previewArea.classList.add('hidden');
    }
}

async function runHexoCommand(command) {
    showStatus(`正在执行 hexo ${command}...`);
    try {
        const res = await fetch(`${API}/posts/${command}`, { method: 'POST' });
        const data = await res.json();
        if (!res.ok) {
            const msg = data.error || data.output || `HTTP ${res.status}`;
            console.error('hexo command failed:', data);
            throw new Error(msg);
        }
        if (parseInt(data.exitCode) !== 0) {
            console.error(data.output);
            throw new Error(data.output || `exitCode=${data.exitCode}`);
        }
        showStatus(`hexo ${command} 执行成功`);
    } catch (err) {
        showStatus(`hexo ${command} 失败：${err.message}`, 'error');
    }
}

function insertImageAtCursor(url) {
    const textarea = el.content;
    const md = `\n![图片描述](${url})\n`;
    if (textarea.selectionStart !== undefined) {
        const start = textarea.selectionStart;
        const end = textarea.selectionEnd;
        textarea.value = textarea.value.substring(0, start) + md + textarea.value.substring(end);
        textarea.selectionStart = textarea.selectionEnd = start + md.length;
    } else {
        textarea.value += md;
    }
    textarea.focus();
}

async function uploadImage(file) {
    const form = new FormData();
    form.append('file', file);
    try {
        const res = await fetch(`${API}/upload`, { method: 'POST', body: form });
        if (!res.ok) throw new Error('上传失败');
        const data = await res.json();
        insertImageAtCursor(data.url);
        showStatus('图片上传成功');
    } catch (err) {
        showStatus('图片上传失败：' + err.message, 'error');
    }
}

// 事件绑定
document.getElementById('btn-new').addEventListener('click', newPost);
document.getElementById('btn-save').addEventListener('click', savePost);
document.getElementById('btn-delete').addEventListener('click', deletePost);
document.getElementById('btn-preview').addEventListener('click', togglePreview);
document.getElementById('btn-generate').addEventListener('click', () => runHexoCommand('generate'));
document.getElementById('btn-clean').addEventListener('click', () => runHexoCommand('clean'));

document.getElementById('btn-upload').addEventListener('click', () => el.fileInput.click());
el.fileInput.addEventListener('change', () => {
    const file = el.fileInput.files[0];
    if (file) uploadImage(file);
    el.fileInput.value = '';
});

el.search.addEventListener('input', filterList);

loadPosts();
