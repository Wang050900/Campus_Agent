/**
 * ============================================
 * 校园 AI 助手 — 小C
 * 功能：聊天交互 / Markdown 渲染 / 复制 / 深色模式
 * ============================================
 */

// ====================================
// 深色模式切换
// ====================================
const THEME_KEY = 'campus-agent-theme';

function initTheme() {
    const saved = localStorage.getItem(THEME_KEY) || 'dark';
    document.documentElement.setAttribute('data-theme', saved);
}

function toggleTheme() {
    const current = document.documentElement.getAttribute('data-theme');
    const next = current === 'dark' ? 'light' : 'dark';
    document.documentElement.setAttribute('data-theme', next);
    localStorage.setItem(THEME_KEY, next);
    renderThemeButton(next);
}

function renderThemeButton(theme) {
    const btn = document.getElementById('theme-toggle');
    if (!btn) return;
    btn.textContent = theme === 'dark' ? '☀️' : '🌙';
    btn.setAttribute('aria-label', theme === 'dark' ? '切换亮色模式' : '切换深色模式');
}

// ====================================
// 简易 Markdown 渲染
// ====================================
function renderMarkdown(text) {
    let html = escapeHtml(text);

    // 代码块 (```code```)
    html = html.replace(/```(\w*)\n([\s\S]*?)```/g, (_, lang, code) => {
        const langClass = lang ? ` class="lang-${escapeHtml(lang)}"` : '';
        return `<pre><code${langClass}>${escapeHtml(code.trim())}</code></pre>`;
    });

    // 行内代码 (`code`)
    html = html.replace(/`([^`]+)`/g, '<code>$1</code>');

    // 加粗 (**text**)
    html = html.replace(/\*\*(.+?)\*\*/g, '<strong>$1</strong>');

    // 无序列表 (- item)
    html = html.replace(/^- (.+)$/gm, '<li>$1</li>');
    
    // 有序列表 (1. item)
    html = html.replace(/^\d+\. (.+)$/gm, '<li>$1</li>');

    // 包裹连续的 li 为 ul/ol
    html = html.replace(/((?:<li>.*?<\/li>\n?)+)/g, '<ul>$1</ul>');

    // 换行转 <br>（不在 pre/code 里的）
    html = html.replace(/\n/g, '<br>');

    // 修复空 <br> 在 ul 里的问题
    html = html.replace(/<br>/g, '\n').replace(/<\/ul>\n<ul>/g, '');
    html = html.replace(/\n/g, '<br>');

    // 来源链接标注：📄 【标题】 → 加样式
    html = html.replace(/【(.+?)】/g, '<span class="source-ref">【$1】</span>');

    return html;
}

// ====================================
// 消息渲染
// ====================================
function appendUserMessage(text) {
    const container = document.getElementById('chat-messages');
    const div = document.createElement('div');
    div.className = 'message user';
    div.innerHTML = `<div class="bubble">${escapeHtml(text)}</div>`;
    container.appendChild(div);
    scrollToBottom();
}

function appendAssistantMessage(text, sources, mode, toolCalls) {
    const container = document.getElementById('chat-messages');
    const div = document.createElement('div');
    div.className = 'message assistant';

    const rendered = renderMarkdown(text);

    let html = `<div class="bubble">${rendered}</div>`;

    // 工具调用卡片
    if (mode === 'TOOL' && toolCalls && toolCalls.length > 0) {
        html += '<div class="tool-calls-section">';
        html += '<div class="tool-calls-badge">🔧 工具调用</div>';
        toolCalls.forEach(tool => {
            html += `
                <div class="tool-card">
                    <div class="tool-header">
                        <span class="tool-name">${escapeHtml(tool.toolName)}</span>
                        <span class="tool-desc">${escapeHtml(tool.description)}</span>
                    </div>
                    <div class="tool-result">${escapeHtml(tool.result || '')}</div>
                </div>
            `;
        });
        html += '</div>';
    }

    // 来源卡片
    if (sources && sources.length > 0) {
        html += '<div class="bubble" style="margin-top: 8px;">';
        html += '<span class="sources-label">📚 文章来源</span>';
        sources.forEach(source => {
            const url = source.url || '#';
            html += `
                <div class="source-card" onclick="window.open('${escapeHtml(url)}', '_blank')">
                    <div class="source-header">
                        <span class="source-tag">来源</span>
                        <a class="source-title" href="${escapeHtml(url)}" target="_blank" rel="noopener" onclick="event.stopPropagation()">
                            ${escapeHtml(source.title)}
                        </a>
                    </div>
                    <div class="source-summary">${escapeHtml(source.summary)}</div>
                </div>
            `;
        });
        html += '</div>';
    }

    // 操作按钮
    html += `<div class="message-actions">
        <button onclick="copyMessage(this)" title="复制回答" aria-label="复制回答">📋</button>
        <button onclick="speakMessage(this)" title="语音朗读" aria-label="语音朗读">🔊</button>
    </div>`;

    div.innerHTML = html;
    container.appendChild(div);
    scrollToBottom();
}

// ====================================
// 复制 & 朗读
// ====================================
function copyMessage(btn) {
    const bubble = btn.closest('.message.assistant').querySelector('.bubble');
    const text = bubble ? bubble.textContent.trim() : '';

    navigator.clipboard.writeText(text).then(() => {
        btn.textContent = '✅';
        btn.classList.add('copied');
        showToast('已复制到剪贴板');
        setTimeout(() => {
            btn.textContent = '📋';
            btn.classList.remove('copied');
        }, 2000);
    }).catch(() => {
        // Fallback for older browsers
        const ta = document.createElement('textarea');
        ta.value = text;
        document.body.appendChild(ta);
        ta.select();
        document.execCommand('copy');
        document.body.removeChild(ta);
        btn.textContent = '✅';
        btn.classList.add('copied');
        showToast('已复制到剪贴板');
        setTimeout(() => {
            btn.textContent = '📋';
            btn.classList.remove('copied');
        }, 2000);
    });
}

function speakMessage(btn) {
    const bubble = btn.closest('.message.assistant').querySelector('.bubble');
    const text = bubble ? bubble.textContent.trim() : '';
    if (!text) return;

    if ('speechSynthesis' in window) {
        window.speechSynthesis.cancel();
        const utterance = new SpeechSynthesisUtterance(text);
        utterance.lang = 'zh-CN';
        utterance.rate = 1.0;
        utterance.pitch = 1.0;
        window.speechSynthesis.speak(utterance);
        showToast('🔊 正在朗读...');
    } else {
        showToast('当前浏览器不支持语音朗读');
    }
}

// ====================================
// Toast 提示
// ====================================
function showToast(message) {
    let toast = document.getElementById('toast');
    if (!toast) {
        toast = document.createElement('div');
        toast.id = 'toast';
        toast.className = 'toast';
        document.body.appendChild(toast);
    }

    toast.textContent = message;
    toast.classList.add('visible');

    clearTimeout(toast._hideTimer);
    toast._hideTimer = setTimeout(() => {
        toast.classList.remove('visible');
    }, 2000);
}

// ====================================
// 加载状态
// ====================================
function showLoading(visible) {
    const indicator = document.getElementById('loading-indicator');
    if (indicator) {
        indicator.style.display = visible ? 'flex' : 'none';
    }
}

// ====================================
// 自动滚动
// ====================================
function scrollToBottom() {
    const chatMain = document.querySelector('.chat-main');
    setTimeout(() => {
        chatMain.scrollTop = chatMain.scrollHeight;
    }, 50);
}

// ====================================
// HTML 转义（XSS 防护）
// ====================================
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

// ====================================
// 核心发送函数
// ====================================
async function sendMessage() {
    const input = document.getElementById('message-input');
    const message = input.value.trim();
    if (!message) return;

    // 清空输入
    input.value = '';
    input.style.height = 'auto';
    input.focus();

    // 移除欢迎卡片
    const welcomeCard = document.querySelector('.welcome-card');
    if (welcomeCard) {
        welcomeCard.style.animation = 'fadeOut 0.3s ease forwards';
        setTimeout(() => welcomeCard.remove(), 300);
    }

    // 渲染用户消息
    appendUserMessage(message);

    // 加载状态
    showLoading(true);

    try {
        const token = localStorage.getItem('token') || '';

        const response = await fetch('/chat', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'Authorization': token ? 'Bearer ' + token : ''
            },
            body: JSON.stringify({ message })
        });

        if (response.status === 401) {
            showLoading(false);
            appendAssistantMessage('⏰ 登录已过期，请重新登录。', null);
            setTimeout(() => { window.location.href = '/login.html'; }, 2000);
            return;
        }

        const data = await response.json();
        showLoading(false);
        appendAssistantMessage(data.answer, data.sources, data.mode, data.toolCalls);

    } catch (error) {
        showLoading(false);
        appendAssistantMessage('抱歉，小C暂时无法回答这个问题。请检查网络连接后重试。', null);
        console.error('请求失败:', error);
    }

    scrollToBottom();
}

// ====================================
// 建议问题
// ====================================
function askSuggestion(text) {
    const input = document.getElementById('message-input');
    input.value = text;
    sendMessage();
}

// ====================================
// 事件绑定
// ====================================
document.addEventListener('DOMContentLoaded', () => {
    // 主题
    initTheme();
    renderThemeButton(document.documentElement.getAttribute('data-theme'));

    const input = document.getElementById('message-input');
    const sendBtn = document.getElementById('send-button');
    const themeBtn = document.getElementById('theme-toggle');

    // 主题切换
    themeBtn.addEventListener('click', toggleTheme);

    // Enter 发送 / Shift+Enter 换行
    input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });

    // 自动调整高度
    input.addEventListener('input', () => {
        input.style.height = 'auto';
        input.style.height = Math.min(input.scrollHeight, 120) + 'px';
    });

    // 发送按钮
    sendBtn.addEventListener('click', sendMessage);
});
