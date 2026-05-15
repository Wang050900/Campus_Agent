/**
 * 校园 AI 助手 — 前端聊天逻辑
 *
 * 功能：
 *   1. 发送消息到后端 /chat API
 *   2. 渲染 AI 回答（支持换行、来源卡片）
 *   3. 加载状态动画
 *   4. Enter 发送 / Shift+Enter 换行
 *   5. 自动滚动到底部
 */

// ====================================
// 核心函数
// ====================================

/**
 * 发送消息
 * 从输入框获取内容，发往后端，渲染回答
 */
async function sendMessage() {
    const input = document.getElementById('message-input');
    const message = input.value.trim();

    if (!message) return;

    // 1. 清空输入框 + 重置高度
    input.value = '';
    input.style.height = 'auto';

    // 2. 移除欢迎卡片（如果有）
    const welcomeCard = document.querySelector('.welcome-card');
    if (welcomeCard) {
        welcomeCard.remove();
    }

    // 3. 渲染用户消息
    appendUserMessage(message);

    // 4. 显示加载状态
    showLoading(true);

    // 5. 调用后端 API
    try {
        const response = await fetch('/chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message })
        });

        if (!response.ok) {
            throw new Error(`HTTP ${response.status}`);
        }

        const data = await response.json();

        // 6. 隐藏加载状态 + 渲染 AI 回答
        showLoading(false);
        appendAssistantMessage(data.answer, data.sources);

    } catch (error) {
        showLoading(false);
        appendAssistantMessage('抱歉，我暂时无法回答这个问题。请检查网络连接后重试。', null);
        console.error('请求失败:', error);
    }

    // 7. 滚动到底部
    scrollToBottom();
}

// ====================================
// 渲染函数
// ====================================

/**
 * 渲染用户消息气泡
 */
function appendUserMessage(text) {
    const container = document.getElementById('chat-messages');
    const div = document.createElement('div');
    div.className = 'message user';
    div.innerHTML = `<div class="bubble">${escapeHtml(text)}</div>`;
    container.appendChild(div);
    scrollToBottom();
}

/**
 * 渲染 AI 回答气泡
 * @param {string} text - AI 回答文本
 * @param {Array|null} sources - 来源文章列表
 */
function appendAssistantMessage(text, sources) {
    const container = document.getElementById('chat-messages');
    const div = document.createElement('div');
    div.className = 'message assistant';

    // 将文本中的换行转成 <br>
    const formattedText = escapeHtml(text).replace(/\n/g, '<br>');

    let html = `<div class="bubble"><p>${formattedText}</p>`;

    // 如果有来源信息，渲染来源卡片
    if (sources && sources.length > 0) {
        html += '<div class="sources-section">';
        sources.forEach(source => {
            html += `
                <div class="source-card">
                    <div class="source-header">
                        <span class="source-icon">📄</span>
                        <a class="source-title" href="${escapeHtml(source.url)}" target="_blank" rel="noopener">
                            ${escapeHtml(source.title)}
                        </a>
                    </div>
                    <div class="source-summary">${escapeHtml(source.summary)}</div>
                </div>
            `;
        });
        html += '</div>';
    }

    html += '</div>';
    div.innerHTML = html;
    container.appendChild(div);
    scrollToBottom();
}

// ====================================
// 辅助函数
// ====================================

/**
 * 显示/隐藏加载状态
 */
function showLoading(visible) {
    const indicator = document.getElementById('loading-indicator');
    indicator.style.display = visible ? 'flex' : 'none';
}

/**
 * 滚动聊天区域到底部
 */
function scrollToBottom() {
    const chatMain = document.querySelector('.chat-main');
    setTimeout(() => {
        chatMain.scrollTop = chatMain.scrollHeight;
    }, 50);
}

/**
 * HTML 转义（防止 XSS）
 */
function escapeHtml(text) {
    const div = document.createElement('div');
    div.textContent = text;
    return div.innerHTML;
}

/**
 * 点击建议问题按钮时触发
 */
function askSuggestion(text) {
    document.getElementById('message-input').value = text;
    sendMessage();
}

// ====================================
// 事件绑定
// ====================================

document.addEventListener('DOMContentLoaded', () => {
    const input = document.getElementById('message-input');
    const sendBtn = document.getElementById('send-button');

    // Enter 发送（Shift+Enter 换行）
    input.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && !e.shiftKey) {
            e.preventDefault();
            sendMessage();
        }
    });

    // 自动调整输入框高度
    input.addEventListener('input', () => {
        input.style.height = 'auto';
        input.style.height = Math.min(input.scrollHeight, 120) + 'px';
    });
});
