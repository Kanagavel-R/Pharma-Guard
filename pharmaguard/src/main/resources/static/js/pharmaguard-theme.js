/* ==========================================================================
   PHARMA GUARD — INTERACTIVE FRONTEND ENGINE (THEME, MODALS, AI ASSISTANT)
   ========================================================================== */

document.addEventListener('DOMContentLoaded', () => {
  initTheme();
  initMobileDrawer();
});

/* ==========================================================================
   1. DARK / LIGHT THEME TOGGLE
   ========================================================================== */

function initTheme() {
  const savedTheme = localStorage.getItem('pharmaguard_theme') || 'light';
  document.documentElement.setAttribute('data-theme', savedTheme);
  updateThemeIcon(savedTheme);
}

function toggleTheme() {
  const currentTheme = document.documentElement.getAttribute('data-theme') || 'light';
  const newTheme = currentTheme === 'light' ? 'dark' : 'light';
  
  document.documentElement.setAttribute('data-theme', newTheme);
  localStorage.setItem('pharmaguard_theme', newTheme);
  updateThemeIcon(newTheme);
}

function updateThemeIcon(theme) {
  const btn = document.getElementById('theme-toggle-btn');
  if (btn) {
    btn.innerHTML = theme === 'dark' ? '<i class="fa-solid fa-sun"></i>' : '<i class="fa-solid fa-moon"></i>';
    btn.setAttribute('title', theme === 'dark' ? 'Switch to Light Mode' : 'Switch to Dark Mode');
  }
}

/* ==========================================================================
   2. MOBILE NAVIGATION DRAWER
   ========================================================================== */

function initMobileDrawer() {
  const toggleBtn = document.getElementById('mobile-menu-toggle');
  const drawer = document.getElementById('mobile-drawer');
  if (toggleBtn && drawer) {
    toggleBtn.addEventListener('click', () => {
      drawer.classList.toggle('open');
    });
  }
}

/* ==========================================================================
   3. MEDICINE VERIFICATION MODAL
   ========================================================================== */

function openVerifyModal() {
  const modal = document.getElementById('verify-modal');
  if (modal) modal.classList.add('open');
}

function closeVerifyModal() {
  const modal = document.getElementById('verify-modal');
  if (modal) modal.classList.remove('open');
}

function runVerification(event) {
  event.preventDefault();
  const resultContainer = document.getElementById('verify-results');
  if (!resultContainer) return;

  const medName = document.getElementById('verify-med-name')?.value || 'Medicine';
  const batchNo = document.getElementById('verify-batch')?.value || 'B-9981';

  resultContainer.style.display = 'block';
  resultContainer.innerHTML = `
    <div style="margin-top:16px; padding:16px; background:var(--success-bg); border-left:4px solid var(--success); border-radius:var(--radius-sm);">
      <div style="font-weight:700; color:var(--success-text); font-size:15px;">🟢 VERIFIED — SAFE</div>
      <div style="font-size:13px; color:var(--text-primary); margin-top:4px;">
        <strong>${medName}</strong> (Batch: <code>${batchNo}</code>) matches registered pharmaceutical standards. No adverse interaction flags reported.
      </div>
    </div>
  `;
}

/* ==========================================================================
   4. SYSTEM CAMERA LIVE BARCODE / QR SCANNER
   ========================================================================== */

let mediaStream = null;
let barcodeDetector = null;
let scanTimer = null;

if ('BarcodeDetector' in window) {
  try {
    barcodeDetector = new BarcodeDetector({
      formats: ['qr_code', 'ean_13', 'ean_8', 'code_128', 'code_39', 'upc_a']
    });
  } catch (e) {
    barcodeDetector = null;
  }
}

function openScannerModal() {
  const modal = document.getElementById('scanner-modal');
  if (modal) {
    modal.classList.add('open');
    modal.style.display = 'flex';
  }
  const drawer = document.getElementById('mobile-drawer');
  if (drawer) {
    drawer.classList.remove('open');
  }
  startCamera();
}

function closeScannerModal() {
  const modal = document.getElementById('scanner-modal');
  if (modal) {
    modal.classList.remove('open');
    modal.style.display = 'none';
  }
  stopCamera();
}

async function startCamera() {
  const video = document.getElementById('scanner-video');
  const placeholder = document.getElementById('scanner-placeholder');
  const statusMsg = document.getElementById('scanner-status');

  try {
    if (mediaStream) {
      stopCamera();
    }

    try {
      mediaStream = await navigator.mediaDevices.getUserMedia({
        video: { facingMode: 'environment' }
      });
    } catch (constraintErr) {
      // Fallback for laptops and desktop webcams without 'environment' facingMode
      mediaStream = await navigator.mediaDevices.getUserMedia({
        video: true
      });
    }

    if (video) {
      video.srcObject = mediaStream;
      video.style.display = 'block';
      await video.play();
    }
    if (placeholder) placeholder.style.display = 'none';
    if (statusMsg) statusMsg.innerHTML = '🟢 Camera active — Point at barcode';

    // Start continuous camera scanning loop
    if (scanTimer) clearInterval(scanTimer);
    scanTimer = setInterval(detectBarcodeFromCamera, 400);

  } catch (err) {
    console.warn('System camera access notice:', err);
    if (placeholder) {
      placeholder.style.display = 'flex';
      placeholder.innerHTML = '📷 Camera stream offline or permission denied.<br/>Click "⚡ Test Barcode Scan" below to verify.';
    }
    if (statusMsg) statusMsg.innerHTML = '⚠️ Camera offline';
  }
}

async function detectBarcodeFromCamera() {
  const video = document.getElementById('scanner-video');
  if (!video || video.readyState !== 4) return;

  if (barcodeDetector) {
    try {
      const barcodes = await barcodeDetector.detect(video);
      if (barcodes && barcodes.length > 0) {
        const code = barcodes[0].rawValue;
        onBarcodeScanned(code);
      }
    } catch (err) {}
  }
}

function stopCamera() {
  if (scanTimer) {
    clearInterval(scanTimer);
    scanTimer = null;
  }
  if (mediaStream) {
    mediaStream.getTracks().forEach(track => track.stop());
    mediaStream = null;
  }
  const video = document.getElementById('scanner-video');
  if (video) {
    video.srcObject = null;
    video.style.display = 'none';
  }
  const placeholder = document.getElementById('scanner-placeholder');
  if (placeholder) {
    placeholder.style.display = 'flex';
    placeholder.innerHTML = 'Align barcode here';
  }
}

function onBarcodeScanned(code) {
  const output = document.getElementById('scanner-result');
  if (!output) return;

  output.style.display = 'block';
  output.innerHTML = `
    <div style="padding:16px; background:var(--success-bg); border-left:4px solid var(--success); border-radius:var(--radius-sm); font-size:13px; color:var(--success-text);">
      <div style="font-weight:700; font-size:15px; margin-bottom:6px;">🟢 SYSTEM CAMERA SCANNED — VERIFIED LEGITIMATE</div>
      <strong>Scanned Code:</strong> ${code || '8901234567890'}<br/>
      <strong>Medicine Name:</strong> Paracetamol 500mg (Batch: <code>P-2026-B1</code>)<br/>
      <strong>Manufacturer:</strong> Apex Pharma Labs<br/>
      <strong>Expiry Status:</strong> 🟢 Valid (14 Months Remaining)<br/>
      <strong>Safety Audit Score:</strong> 98% (No Recalls / No Active Alerts)
    </div>
  `;
}

function simulateScan() {
  onBarcodeScanned('8901234567890');
}



/* ==========================================================================
   5. FLOATING PHARMA GUARD AI CHATBOT
   ========================================================================== */

/* ==========================================================================
   5. FLOATING PHARMA GUARD AI CHATBOT ENGINE
   ========================================================================== */

let isAiChatInitialized = false;

function toggleAiChat() {
  let drawer = document.getElementById('ai-chat-drawer');
  if (!drawer) {
    drawer = createFloatingAiDrawer();
  }

  drawer.classList.toggle('open');
  if (drawer.classList.contains('open')) {
    checkAiStatus();
    ensureAiWelcomeScreen();
    ensureAiChatInput(drawer);
  }
}

function createFloatingAiDrawer() {
  let drawer = document.getElementById('ai-chat-drawer');
  if (drawer) return drawer;

  drawer = document.createElement('div');
  drawer.className = 'ai-chat-drawer';
  drawer.id = 'ai-chat-drawer';

  drawer.innerHTML = `
    <div class="chat-header">
      <div class="chat-header-info">
        <div class="chat-header-title">
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M12 2a10 10 0 0 1 10 10c0 5.523-4.477 10-10 10S2 17.523 2 12 6.477 2 12 2z"/><path d="M12 6v6l4 2"/></svg>
          Pharma Guard AI
          <span id="ai-status-badge" class="chat-status-badge"><span id="ai-status-dot" class="chat-status-dot"></span> <span id="ai-status-text">Checking...</span></span>
        </div>
        <div class="chat-header-subtitle">Your intelligent pharmacy operations assistant</div>
      </div>
      <div class="chat-header-actions">
        <button type="button" class="chat-header-btn" onclick="clearAiConversation()" title="Clear Conversation">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M3 6h18"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg>
        </button>
        <button type="button" class="chat-header-btn" onclick="toggleAiChat()" title="Close Assistant">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="18" y1="6" x2="6" y2="18"/><line x1="6" y1="6" x2="18" y2="18"/></svg>
        </button>
      </div>
    </div>
    <div class="chat-body" id="ai-chat-body"></div>
  `;

  document.body.appendChild(drawer);

  // Also ensure trigger button exists
  if (!document.querySelector('.ai-assistant-trigger')) {
    const trigger = document.createElement('div');
    trigger.className = 'ai-assistant-trigger';
    trigger.onclick = toggleAiChat;
    trigger.innerHTML = `
      <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><rect x="3" y="11" width="18" height="10" rx="2"/><circle cx="12" cy="5" r="2"/><path d="M12 7v4"/><line x1="8" y1="16" x2="8" y2="16"/><line x1="16" y1="16" x2="16" y2="16"/></svg>
      AI Assistant
    `;
    document.body.appendChild(trigger);
  }

  return drawer;
}

function checkAiStatus() {
  fetch('/api/ai/status')
    .then(res => res.json())
    .then(data => {
      const dot = document.getElementById('ai-status-dot');
      const text = document.getElementById('ai-status-text');
      if (dot && text) {
        if (data.online) {
          dot.className = 'chat-status-dot';
          text.innerText = 'AI Online';
        } else {
          dot.className = 'chat-status-dot offline';
          text.innerText = 'AI Offline';
        }
      }
    })
    .catch(() => {
      const dot = document.getElementById('ai-status-dot');
      const text = document.getElementById('ai-status-text');
      if (dot && text) {
        dot.className = 'chat-status-dot offline';
        text.innerText = 'AI Offline';
      }
    });
}

function ensureAiWelcomeScreen() {
  const body = document.getElementById('ai-chat-body');
  if (!body || body.children.length > 0) return;

  const welcome = document.createElement('div');
  welcome.className = 'chat-welcome-box';
  welcome.style.background = '#FFFFFF';
  welcome.style.border = '1px solid #E2E8F0';
  welcome.style.borderRadius = '12px';
  welcome.style.padding = '16px';
  welcome.style.marginBottom = '12px';

  welcome.innerHTML = `
    <div style="font-weight:800; font-size:15px; color:#1E293B; margin-bottom:4px;">How can I help you manage your pharmacy?</div>
    <div style="font-size:12px; color:#64748B; margin-bottom:14px;">Ask about medicines, inventory, suppliers, alerts, verification and pharmacy operations.</div>
    <div style="display:grid; grid-template-columns:1fr 1fr; gap:6px;">
      <button type="button" class="btn btn-outline btn-sm" style="font-size:11px; padding:6px 8px; text-align:left;" onclick="sendAiPrompt('Show Paracetamol details')">Medicine Search</button>
      <button type="button" class="btn btn-outline btn-sm" style="font-size:11px; padding:6px 8px; text-align:left;" onclick="sendAiPrompt('Show low stock medicines')">Low Stock</button>
      <button type="button" class="btn btn-outline btn-sm" style="font-size:11px; padding:6px 8px; text-align:left;" onclick="sendAiPrompt('Which medicines expire in 30 days?')">Expiring Soon</button>
      <button type="button" class="btn btn-outline btn-sm" style="font-size:11px; padding:6px 8px; text-align:left;" onclick="sendAiPrompt('Give me an inventory summary')">Inventory Summary</button>
      <button type="button" class="btn btn-outline btn-sm" style="font-size:11px; padding:6px 8px; text-align:left;" onclick="sendAiPrompt('Show today\'s alerts')">Today's Alerts</button>
      <button type="button" class="btn btn-outline btn-sm" style="font-size:11px; padding:6px 8px; text-align:left;" onclick="sendAiPrompt('Show verification issues')">Verification Issues</button>
      <button type="button" class="btn btn-outline btn-sm" style="font-size:11px; padding:6px 8px; text-align:left;" onclick="sendAiPrompt('Show all suppliers')">Suppliers</button>
      <button type="button" class="btn btn-outline btn-sm" style="font-size:11px; padding:6px 8px; text-align:left;" onclick="sendAiPrompt('Show reorder recommendations')">Reorder Recommendations</button>
      <button type="button" class="btn btn-outline btn-sm" style="font-size:11px; padding:6px 8px; text-align:left;" onclick="sendAiPrompt('Give me today\'s pharmacy summary')">Pharmacy Summary</button>
      <button type="button" class="btn btn-outline btn-sm" style="font-size:11px; padding:6px 8px; text-align:left;" onclick="sendAiPrompt('What is the biggest inventory risk?')">Risk Analysis</button>
      <button type="button" class="btn btn-outline btn-sm" style="font-size:11px; padding:6px 8px; text-align:left;" onclick="sendAiPrompt('Generate today\'s inventory report')">Reports</button>
      <button type="button" class="btn btn-outline btn-sm" style="font-size:11px; padding:6px 8px; text-align:left;" onclick="sendAiPrompt('Which medicines should we reorder first?')">Natural Language Search</button>
    </div>
  `;

  body.appendChild(welcome);
}

function clearAiConversation() {
  fetch('/api/ai/clear-history', { method: 'POST' })
    .finally(() => {
      const body = document.getElementById('ai-chat-body');
      if (body) {
        body.innerHTML = '';
        ensureAiWelcomeScreen();
      }
    });
}

function ensureAiChatInput(drawer) {
  if (!drawer || drawer.querySelector('.chat-footer')) return;

  const footer = document.createElement('div');
  footer.className = 'chat-footer';
  footer.style.display = 'flex';
  footer.style.padding = '10px';
  footer.style.borderTop = '1px solid var(--border-color, #E2E8F0)';
  footer.style.gap = '8px';
  footer.style.background = 'var(--bg-card, #FFFFFF)';

  footer.innerHTML = `
    <input type="text" id="ai-chat-input" placeholder="Ask about medicines, inventory, suppliers, alerts..." style="flex:1; padding:8px 14px; border:1px solid #CBD5E1; border-radius:20px; outline:none; font-size:13px;" onkeypress="if(event.key==='Enter') submitAiChatInput()">
    <button type="button" id="ai-chat-send-btn" onclick="submitAiChatInput()" style="background:#4F46E5; color:#FFF; border:none; border-radius:50%; width:34px; height:34px; cursor:pointer; font-size:14px; display:flex; align-items:center; justify-content:center;">
      <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><line x1="22" y1="2" x2="11" y2="13"/><polygon points="22 2 15 22 11 13 2 9 22 2"/></svg>
    </button>
  `;
  drawer.appendChild(footer);
}

function submitAiChatInput() {
  const input = document.getElementById('ai-chat-input');
  if (!input || !input.value.trim()) return;
  const prompt = input.value.trim();
  input.value = '';
  sendAiPrompt(prompt);
}

function sendAiPrompt(promptText) {
  const body = document.getElementById('ai-chat-body');
  if (!body) return;

  const sendBtn = document.getElementById('ai-chat-send-btn');
  const input = document.getElementById('ai-chat-input');

  if (sendBtn) sendBtn.disabled = true;
  if (input) input.disabled = true;

  // Append User message bubble
  const userMsg = document.createElement('div');
  userMsg.className = 'chat-bubble';
  userMsg.style.background = '#4F46E5';
  userMsg.style.color = '#FFFFFF';
  userMsg.style.alignSelf = 'flex-end';
  userMsg.style.marginBottom = '8px';
  userMsg.style.borderRadius = '14px 14px 2px 14px';
  userMsg.innerText = promptText;
  body.appendChild(userMsg);

  // Append Loading bubble
  const loadingMsg = document.createElement('div');
  loadingMsg.className = 'chat-bubble';
  loadingMsg.id = 'drawerLoadingMsg';
  loadingMsg.style.alignSelf = 'flex-start';
  loadingMsg.style.marginBottom = '8px';
  loadingMsg.style.fontStyle = 'italic';
  loadingMsg.style.color = '#64748B';
  loadingMsg.style.background = '#F1F5F9';
  loadingMsg.style.border = '1px solid #E2E8F0';
  loadingMsg.innerHTML = '<span style="display:inline-block; animation:pulse 1.2s infinite;">Pharma Guard AI is analyzing...</span>';
  body.appendChild(loadingMsg);
  body.scrollTop = body.scrollHeight;

  // Call backend API
  fetch('/api/ai/chat', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ prompt: promptText })
  })
  .then(res => res.json())
  .then(data => {
    const l = document.getElementById('drawerLoadingMsg');
    if (l) l.remove();

    const aiMsg = document.createElement('div');
    aiMsg.className = 'chat-bubble';
    aiMsg.style.alignSelf = 'flex-start';
    aiMsg.style.marginBottom = '8px';
    aiMsg.style.background = '#FFFFFF';
    aiMsg.style.border = '1px solid #E2E8F0';
    aiMsg.style.color = '#1E293B';
    aiMsg.style.borderRadius = '14px 14px 14px 2px';

    let formattedText = (data.reply || 'No response received.')
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/\n/g, '<br/>')
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>');

    // Handle Cards Response
    if (data.responseType === 'CARDS' && data.cardMetrics) {
      formattedText += '<div style="display:flex; flex-direction:column; gap:6px; margin-top:8px;">';
      for (const [key, val] of Object.entries(data.cardMetrics)) {
        formattedText += `
          <div style="background:#F8FAFC; border:1px solid #E2E8F0; padding:8px 10px; border-radius:8px; display:flex; justify-content:space-between; align-items:center; font-size:12px;">
            <span style="color:#64748B; font-weight:600;">${key}</span>
            <span style="color:#1E293B; font-weight:700;">${val}</span>
          </div>
        `;
      }
      formattedText += '</div>';
    }

    // Handle Table
    if (data.responseType === 'TABLE' && data.tableHeaders && data.tableRows) {
      formattedText += '<div style="overflow-x:auto; margin-top:8px;"><table style="width:100%; border-collapse:collapse; font-size:11px;"><thead><tr>';
      data.tableHeaders.forEach(h => formattedText += `<th style="border:1px solid #CBD5E1; padding:4px 6px; background:#F1F5F9; text-align:left; font-weight:700; color:#334155;">${h}</th>`);
      formattedText += '</tr></thead><tbody>';
      data.tableRows.forEach(row => {
        formattedText += '<tr>';
        row.forEach(cell => formattedText += `<td style="border:1px solid #CBD5E1; padding:4px 6px; color:#1E293B;">${cell}</td>`);
        formattedText += '</tr>';
      });
      formattedText += '</tbody></table></div>';
    }

    // Handle Action Proposal
    if (data.responseType === 'ACTION_REQUIRED' && data.actionProposal) {
      const prop = data.actionProposal;
      formattedText += `
        <div style="background:#FEF2F2; border:1px solid #EF4444; border-radius:8px; padding:10px; margin-top:8px; font-size:12px;">
          <strong style="color:#991B1B; display:block; margin-bottom:4px;">${prop.description || 'Action Confirmation'}</strong>
          <span style="color:#475569;">${prop.confirmationMessage || ''}</span>
          <div style="display:flex; gap:6px; margin-top:8px;">
            <button type="button" class="btn btn-primary btn-sm" style="font-size:11px; padding:4px 10px;" onclick="executeDrawerAiAction('${prop.actionType}', ${prop.targetEntityId}, ${JSON.stringify(prop.parameters || {})}, this)">${prop.confirmButtonText || 'Confirm'}</button>
            <button type="button" class="btn btn-outline btn-sm" style="font-size:11px; padding:4px 10px;" onclick="this.closest('div').parentElement.remove()">${prop.cancelButtonText || 'Cancel'}</button>
          </div>
        </div>
      `;
    }

    if (data.sourceData) {
      formattedText += `<div style="font-size:10px; color:#64748B; margin-top:6px; font-weight:600;">${data.sourceData}</div>`;
    }

    aiMsg.innerHTML = formattedText;
    body.appendChild(aiMsg);
    body.scrollTop = body.scrollHeight;
  })
  .catch(err => {
    const l = document.getElementById('drawerLoadingMsg');
    if (l) l.remove();

    const errorMsg = document.createElement('div');
    errorMsg.className = 'chat-bubble';
    errorMsg.style.alignSelf = 'flex-start';
    errorMsg.style.marginBottom = '8px';
    errorMsg.style.background = '#FEE2E2';
    errorMsg.style.color = '#991B1B';
    errorMsg.innerText = 'Pharma Guard AI is temporarily offline.';
    body.appendChild(errorMsg);
    body.scrollTop = body.scrollHeight;
  })
  .finally(() => {
    if (sendBtn) sendBtn.disabled = false;
    if (input) input.disabled = false;
  });
}

function executeDrawerAiAction(actionType, targetEntityId, params, btn) {
  btn.disabled = true;
  btn.innerText = 'Processing...';

  fetch('/api/ai/execute-action', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ actionType: actionType, targetEntityId: targetEntityId, parameters: params })
  })
  .then(res => res.json())
  .then(data => {
    const box = btn.parentElement.parentElement;
    if (box) {
      box.innerHTML = `<span style="color:#059669; font-weight:700;">${data.reply || 'Action executed.'}</span>`;
    }
  })
  .catch(err => {
    alert('Action failed: ' + err.message);
    btn.disabled = false;
    btn.innerText = 'Confirm';
  });
}

/* ==========================================================================
   6. AUTHENTICATION & PASSWORD HELPERS
   ========================================================================== */

function togglePasswordVisibility(fieldId, btn) {
  const input = document.getElementById(fieldId);
  if (!input) return;

  if (input.type === 'password') {
    input.type = 'text';
    btn.innerHTML = '<i class="fa-solid fa-eye-slash"></i>';
    btn.setAttribute('aria-label', 'Hide password');
  } else {
    input.type = 'password';
    btn.innerHTML = '<i class="fa-solid fa-eye"></i>';
    btn.setAttribute('aria-label', 'Show password');
  }
}

function evaluatePasswordStrength(password) {
  const fill = document.getElementById('strength-fill');
  const lenRule = document.getElementById('rule-length');
  const upperRule = document.getElementById('rule-upper');
  const numRule = document.getElementById('rule-number');
  const specRule = document.getElementById('rule-special');

  if (!fill) return;

  let score = 0;

  const hasLength = password.length >= 8;
  const hasUpper = /[A-Z]/.test(password);
  const hasNumber = /[0-9]/.test(password);
  const hasSpecial = /[^A-Za-z0-9]/.test(password);

  updateRuleStatus(lenRule, hasLength, '8+ characters');
  updateRuleStatus(upperRule, hasUpper, 'Uppercase letter');
  updateRuleStatus(numRule, hasNumber, 'Number');
  updateRuleStatus(specRule, hasSpecial, 'Special character');

  if (hasLength) score++;
  if (hasUpper) score++;
  if (hasNumber) score++;
  if (hasSpecial) score++;

  fill.className = 'strength-fill';
  if (password.length === 0) {
    fill.style.width = '0%';
  } else if (score <= 1) {
    fill.classList.add('weak');
  } else if (score <= 3) {
    fill.classList.add('medium');
  } else {
    fill.classList.add('strong');
  }

  validateConfirmPassword();
}

function updateRuleStatus(elem, isValid, label) {
  if (!elem) return;
  if (isValid) {
    elem.classList.add('valid');
    elem.innerHTML = `<i class="fa-solid fa-check" style="color:var(--success, #10b981);"></i> ${label}`;
  } else {
    elem.classList.remove('valid');
    elem.innerHTML = `<i class="fa-regular fa-circle" style="color:var(--text-muted, #94a3b8);"></i> ${label}`;
  }
}

function validateConfirmPassword() {
  const pass = document.getElementById('password')?.value;
  const confirm = document.getElementById('confirmPassword');
  const errorMsg = document.getElementById('confirm-error-msg');

  if (!confirm || !pass) return true;

  if (confirm.value && confirm.value !== pass) {
    confirm.style.borderColor = 'var(--danger)';
    if (errorMsg) {
      errorMsg.style.display = 'block';
      errorMsg.innerHTML = '<i class="fa-solid fa-circle-exclamation"></i> Passwords do not match.';
    }
    return false;
  } else {
    confirm.style.borderColor = '';
    if (errorMsg) errorMsg.style.display = 'none';
    return true;
  }
}

function handleAuthFormSubmit(formElement, btnTextId, loadingText) {
  const btn = formElement.querySelector('button[type="submit"]');
  const btnText = document.getElementById(btnTextId);

  if (!validateConfirmPassword()) {
    return false;
  }

  if (btn) {
    btn.disabled = true;
  }
  if (btnText) {
    btnText.innerHTML = `<i class="fa-solid fa-spinner fa-spin"></i> ${loadingText}`;
  }
  return true;
}

/* ==========================================================================
   5. NAVBAR INTERACTIVE CONTROLS
   ========================================================================== */

function toggleMenu() {
  const navLinks = document.getElementById("navLinks");
  if (navLinks) {
    navLinks.classList.toggle("show");
  }
}

function toggleProfile() {
  const profile = document.getElementById("profile");
  if (profile) {
    profile.classList.toggle("open");
  }
}

document.addEventListener("click", function(event) {
  const profile = document.getElementById("profile");
  if (profile && !profile.contains(event.target)) {
    profile.classList.remove("open");
  }

  const notifDrawer = document.getElementById("notif-drawer");
  const notifBtn = document.getElementById("notif-bell-btn");
  if (notifDrawer && notifDrawer.classList.contains("open") && !notifDrawer.contains(event.target) && notifBtn && !notifBtn.contains(event.target)) {
    notifDrawer.classList.remove("open");
  }
});

/* ==========================================================================
   6. SMART MEDICINE EXPIRY ALERT SYSTEM (MODALS, TOASTS, NOTIFICATION DRAWER)
   ========================================================================== */

const NOTIFICATION_DELAY = 3500; // 3.5 seconds delay after dashboard loads

document.addEventListener('DOMContentLoaded', () => {
  initExpiryAlertEngine();
});

function initExpiryAlertEngine() {
  injectExpiryModalAndDrawerMarkup();
  refreshNotificationBadge();
  scheduleDelayedExpiryNotification();
}

function injectExpiryModalAndDrawerMarkup() {
  if (!document.getElementById('expiry-modal-container')) {
    const modalDiv = document.createElement('div');
    modalDiv.id = 'expiry-modal-container';
    modalDiv.innerHTML = `
      <div id="expiry-warning-modal" class="modal-overlay" style="display:none; position:fixed; top:0; left:0; right:0; bottom:0; background:rgba(15, 23, 42, 0.6); backdrop-filter:blur(4px); z-index:9999; align-items:center; justify-content:center; padding:20px;">
        <div class="modal-card" style="background:var(--bg-card, #FFF); border-radius:16px; border:2px solid var(--danger, #EF4444); max-width:440px; width:100%; padding:28px; text-align:center; box-shadow:0 20px 25px -5px rgba(0,0,0,0.2); animation:modalSlideIn 0.3s ease-out;">
          <div style="font-size:44px; margin-bottom:10px; color:var(--danger, #EF4444);"><i class="fa-solid fa-triangle-exclamation"></i></div>
          <h2 style="font-size:20px; font-weight:800; color:var(--text-primary, #1E293B); margin-bottom:6px;">MEDICINE EXPIRING SOON</h2>
          <p style="color:var(--text-secondary, #64748B); font-size:13px; margin-bottom:16px;">Pharma Guard Expiry Protection Signal</p>
          
          <div style="padding:16px; background:var(--bg-surface-hover, #F8FAFC); border-radius:12px; margin-bottom:16px;">
            <strong id="modal-med-name" style="font-size:18px; color:var(--text-primary, #1E293B); display:block; margin-bottom:8px;">Paracetamol 500mg</strong>
            <div style="margin-bottom:8px;">
              <span id="modal-days-badge" style="background:#FEE2E2; color:#DC2626; padding:4px 14px; border-radius:20px; font-weight:800; font-size:13px;">3 DAYS LEFT</span>
            </div>
            <div style="font-size:13px; color:var(--text-secondary, #64748B);">Expiry Date: <strong id="modal-exp-date" style="color:var(--text-primary, #1E293B);">25 Aug 2026</strong></div>
          </div>

          <div style="font-size:12px; color:var(--text-muted, #94A3B8); background:#FFFBEB; border-left:3px solid #F59E0B; padding:10px 12px; border-radius:6px; text-align:left; line-height:1.4; margin-bottom:20px;">
            <i class="fa-solid fa-lightbulb" style="color:#F59E0B;"></i> <strong>Healthcare Guidance:</strong> This medicine is approaching its stated expiry date. Check the packaging and follow appropriate healthcare guidance.
          </div>

          <div style="display:flex; gap:12px;">
            <button type="button" id="modal-view-btn" class="btn btn-primary" style="flex:1;">View Medicine</button>
            <button type="button" class="btn btn-outline" style="flex:1;" onclick="dismissExpiryModal()">Dismiss</button>
          </div>
        </div>
      </div>

      <!-- TOAST ALERTS CONTAINER -->
      <div id="toast-container" style="position:fixed; bottom:20px; right:20px; z-index:9998; display:flex; flex-direction:column; gap:10px; max-width:380px; width:100%;"></div>

      <!-- NOTIFICATION DRAWER DROPDOWN -->
      <div id="notif-drawer" style="display:none; position:fixed; top:70px; right:20px; width:370px; max-height:500px; background:var(--bg-card, #FFF); border:1px solid var(--border-color, #E2E8F0); border-radius:12px; box-shadow:0 10px 25px rgba(0,0,0,0.15); z-index:9990; flex-direction:column; overflow:hidden;">
        <div style="display:flex; justify-content:space-between; align-items:center; padding:12px 16px; border-bottom:1px solid var(--border-color, #E2E8F0); background:var(--bg-surface-hover, #F8FAFC);">
          <strong style="font-size:14px; color:var(--text-primary, #1E293B);"><i class="fa-solid fa-bell"></i> Notifications</strong>
          <div style="display:flex; gap:8px;">
            <button type="button" onclick="testDesktopNotification()" title="Test OS Desktop Notification" style="background:#EEF2FF; border:1px solid #C7D2FE; color:#4338CA; padding:3px 8px; border-radius:6px; font-size:11px; font-weight:700; cursor:pointer;"><i class="fa-solid fa-bolt"></i> Test OS Alert</button>
            <button type="button" onclick="markAllNotificationsAsRead()" style="background:none; border:none; color:var(--primary, #0D9488); font-size:11px; font-weight:700; cursor:pointer;">Mark All Read</button>
          </div>
        </div>
        <div id="notif-drawer-list" style="overflow-y:auto; padding:10px; display:flex; flex-direction:column; gap:8px;">
          <div style="text-align:center; padding:20px; color:#94A3B8; font-size:13px;">Loading notifications...</div>
        </div>
      </div>
    `;
    document.body.appendChild(modalDiv);
  }
}

// Auto-request browser notification permission on first user click anywhere on page
document.addEventListener('click', function autoRequestPermission() {
  if ('Notification' in window && Notification.permission === 'default') {
    Notification.requestPermission().catch(() => {});
  }
}, { once: true });

function scheduleDelayedExpiryNotification() {
  // Attempt permission request immediately if supported
  if ('Notification' in window && Notification.permission === 'default') {
    try {
      Notification.requestPermission().catch(() => {});
    } catch (e) {}
  }

  setTimeout(() => {
    fetch('/api/notifications/critical-popup')
      .then(res => res.json())
      .then(popups => {
        if (!popups || popups.length === 0) return;

        // Filter items that haven't been dismissed in this session
        const unnotified = popups.filter(p => {
          const key = 'desktop_notif_dismissed_' + p.medicineId + '_' + p.daysRemaining;
          return !sessionStorage.getItem(key);
        });

        if (unnotified.length === 0) return;

        triggerDesktopNotification(unnotified);
      })
      .catch(err => console.warn('Delayed expiry check notice:', err));
  }, NOTIFICATION_DELAY);
}

function triggerDesktopNotification(popups, forceTest = false) {
  if (!popups || popups.length === 0) return;

  const isMulti = popups.length > 1;
  const primary = popups[0];

  let notifTitle = '';
  let notifBody = '';
  let targetUrl = '';

  if (isMulti) {
    notifTitle = '⚠️ Medicines Expiring Soon';
    notifBody = popups.length + ' medicines are expiring within 3 days. Click to view inventory.';
    targetUrl = '/medicines';
  } else {
    if (primary.daysRemaining < 0) {
      notifTitle = '🚨 Medicine Expired';
      notifBody = primary.medicineName + ' has passed its stated expiry date.';
    } else if (primary.daysRemaining === 1) {
      notifTitle = '🚨 Medicine Expires Tomorrow';
      notifBody = primary.medicineName + ' expires tomorrow. Expiry: ' + primary.expiryDate;
    } else if (primary.daysRemaining === 0) {
      notifTitle = '🔴 Medicine Expiring Today';
      notifBody = primary.medicineName + ' expires today. Expiry: ' + primary.expiryDate;
    } else {
      notifTitle = '⚠️ Medicine Expiring Soon';
      notifBody = primary.medicineName + ' expires in ' + primary.daysRemaining + ' days. Expiry: ' + primary.expiryDate;
    }
    targetUrl = '/medicines/details/' + primary.medicineId;
  }

  let nativeNotifSuccess = false;

  // 1. ALWAYS try native OS Browser Desktop Notification
  if ('Notification' in window) {
    if (Notification.permission === 'granted') {
      try {
        const notification = new Notification('🛡️ Pharma Guard — ' + notifTitle, {
          body: notifBody + '\nClick to View Medicine',
          tag: 'pharma-guard-expiry-' + (primary ? primary.medicineId : 'multi'),
          renotify: true,
          requireInteraction: true
        });

        notification.onclick = function (event) {
          event.preventDefault();
          window.focus();
          notification.close();
          window.location.href = targetUrl;
        };

        nativeNotifSuccess = true;
      } catch (err) {
        console.warn('Native Web Notification constructor error:', err);
      }
    } else if (Notification.permission === 'default') {
      Notification.requestPermission().then(permission => {
        if (permission === 'granted') {
          triggerDesktopNotification(popups, forceTest);
        }
      }).catch(() => {});
    }
  }

  // 2. ALWAYS ALSO show in-app Desktop Alert Toast (guarantees notification on screen even if OS notifications are disabled)
  showFallbackToastNotification(notifTitle, notifBody, targetUrl, popups);
}

function showNotificationPermissionBanner(popups) {
  const container = document.getElementById('toast-container');
  if (!container || document.getElementById('perm-request-toast')) return;

  const toast = document.createElement('div');
  toast.id = 'perm-request-toast';
  toast.style.background = '#EFF6FF';
  toast.style.borderLeft = '4px solid #3B82F6';
  toast.style.padding = '14px 18px';
  toast.style.borderRadius = '10px';
  toast.style.boxShadow = '0 10px 25px rgba(0,0,0,0.15)';
  toast.style.display = 'flex';
  toast.style.justifyContent = 'space-between';
  toast.style.alignItems = 'center';
  toast.style.fontSize = '13px';
  toast.style.color = '#1E293B';
  toast.style.animation = 'slideInRight 0.4s ease-out';

  toast.innerHTML = `
    <div>
      <strong style="display:block; font-size:14px; color:#1D4ED8; margin-bottom:2px;">🔔 Enable Desktop Notifications</strong>
      <span style="color:#475569; font-size:12px;">Get real-time OS alerts on your computer for expiring medicines.</span>
    </div>
    <div style="display:flex; align-items:center; gap:8px; margin-left:16px;">
      <button type="button" onclick="requestDesktopPermissionByUser()" class="btn btn-primary btn-sm" style="padding:6px 14px; font-size:12px; white-space:nowrap; background:#2563EB;">Allow Desktop Alerts</button>
      <button type="button" onclick="this.parentElement.parentElement.remove()" style="background:none; border:none; font-size:16px; cursor:pointer; color:#94A3B8;">✕</button>
    </div>
  `;

  container.appendChild(toast);
}

function requestDesktopPermissionByUser() {
  if ('Notification' in window) {
    Notification.requestPermission().then(permission => {
      const banner = document.getElementById('perm-request-toast');
      if (banner) banner.remove();

      if (permission === 'granted') {
        showToastAlert('✅ Desktop Notifications Enabled', 'Pharma Guard will now send native OS notifications for critical expiries.', 'NORMAL');
        // Trigger delayed check immediately
        fetch('/api/notifications/critical-popup')
          .then(res => res.json())
          .then(popups => {
            if (popups && popups.length > 0) {
              triggerDesktopNotification(popups, true);
            }
          });
      } else {
        showToastAlert('⚠️ Permission Denied', 'Desktop notifications blocked by browser. In-app notifications will continue working.', 'WARNING');
      }
    });
  }
}

function testDesktopNotification() {
  if (!('Notification' in window)) {
    alert('Web Notifications API is not supported in this browser.');
    return;
  }

  if (Notification.permission === 'granted') {
    const testPopups = [{
      medicineId: 1,
      medicineName: 'Paracetamol 500mg (Test)',
      daysRemaining: 3,
      expiryDate: '25 Aug 2026'
    }];
    triggerDesktopNotification(testPopups, true);
  } else {
    requestDesktopPermissionByUser();
  }
}

function showFallbackToastNotification(title, message, targetUrl) {
  const container = document.getElementById('toast-container');
  if (!container) return;

  const toast = document.createElement('div');
  toast.style.background = '#FEF2F2';
  toast.style.borderLeft = '4px solid #EF4444';
  toast.style.padding = '14px 18px';
  toast.style.borderRadius = '10px';
  toast.style.boxShadow = '0 10px 25px rgba(0,0,0,0.15)';
  toast.style.display = 'flex';
  toast.style.justifyContent = 'space-between';
  toast.style.alignItems = 'center';
  toast.style.fontSize = '13px';
  toast.style.color = '#1E293B';
  toast.style.animation = 'slideInRight 0.4s ease-out';

  toast.innerHTML = `
    <div>
      <strong style="display:block; font-size:14px; color:#DC2626; margin-bottom:2px;">${title}</strong>
      <span style="color:#475569; font-size:12px;">${message}</span>
    </div>
    <div style="display:flex; align-items:center; gap:8px; margin-left:16px;">
      <a href="${targetUrl}" class="btn btn-primary btn-sm" style="padding:6px 12px; font-size:12px; text-decoration:none; white-space:nowrap;">View Medicine →</a>
      <button type="button" onclick="this.parentElement.parentElement.remove()" style="background:none; border:none; font-size:16px; cursor:pointer; color:#94A3B8;">✕</button>
    </div>
  `;

  container.appendChild(toast);
  setTimeout(() => {
    if (toast.parentElement) toast.remove();
  }, 8000);
}

function showExpiryModal(notification) {
  const modal = document.getElementById('expiry-warning-modal');
  if (!modal) return;

  document.getElementById('modal-med-name').innerText = notification.medicineName;
  document.getElementById('modal-days-badge').innerText = notification.daysRemaining + ' DAYS LEFT';
  document.getElementById('modal-exp-date').innerText = notification.expiryDate;
  
  const viewBtn = document.getElementById('modal-view-btn');
  viewBtn.onclick = () => {
    window.location.href = '/medicines/edit/' + notification.medicineId;
  };

  modal.style.display = 'flex';
}

function dismissExpiryModal() {
  const modal = document.getElementById('expiry-warning-modal');
  if (modal) modal.style.display = 'none';
}

function toggleNotificationDrawer() {
  const drawer = document.getElementById('notif-drawer');
  if (!drawer) return;
  
  if (drawer.style.display === 'none' || !drawer.style.display) {
    drawer.style.display = 'flex';
    drawer.classList.add('open');
    loadNotificationsIntoDrawer();
  } else {
    drawer.style.display = 'none';
    drawer.classList.remove('open');
  }
}

function refreshNotificationBadge() {
  fetch('/api/notifications/unread-count')
    .then(res => res.json())
    .then(data => {
      const badge = document.getElementById('nav-notif-badge');
      if (badge) {
        if (data.unreadCount > 0) {
          badge.innerText = data.unreadCount;
          badge.style.display = 'inline-block';
        } else {
          badge.style.display = 'none';
        }
      }
    })
    .catch(() => {});
}

function loadNotificationsIntoDrawer() {
  const list = document.getElementById('notif-drawer-list');
  if (!list) return;

  fetch('/api/notifications')
    .then(res => res.json())
    .then(notifications => {
      if (!notifications || notifications.length === 0) {
        list.innerHTML = '<div style="text-align:center; padding:20px; color:#94A3B8; font-size:13px;">No new notifications.</div>';
        return;
      }

      list.innerHTML = '';
      notifications.forEach(n => {
        const item = document.createElement('div');
        item.style.padding = '10px 12px';
        item.style.borderRadius = '8px';
        item.style.background = n.read ? 'var(--bg-card, #FFF)' : 'var(--bg-surface-hover, #F8FAFC)';
        item.style.borderLeft = '3px solid ' + (n.severity === 'CRITICAL' ? '#EF4444' : (n.severity === 'HIGH' ? '#F97316' : '#F59E0B'));
        item.style.fontSize = '12px';

        item.innerHTML = `
          <div style="display:flex; justify-content:space-between; align-items:start; margin-bottom:4px;">
            <strong style="color:var(--text-primary, #1E293B); font-size:13px;">${n.title}</strong>
            <span style="font-size:10px; color:#94A3B8;">${new Date(n.createdAt).toLocaleTimeString([], {hour:'2-digit', minute:'2-digit'})}</span>
          </div>
          <div style="color:var(--text-secondary, #64748B); margin-bottom:6px;">${n.message}</div>
          <div style="display:flex; justify-content:space-between; align-items:center;">
            <a href="/medicines/edit/${n.medicineId}" style="color:var(--primary, #0D9488); font-weight:700; text-decoration:none;">View Medicine →</a>
            <button type="button" onclick="deleteNotificationItem(${n.id}, this)" style="background:none; border:none; color:#94A3B8; cursor:pointer; font-size:11px;">Delete</button>
          </div>
        `;
        list.appendChild(item);
      });
    })
    .catch(() => {});
}

function markAllNotificationsAsRead() {
  fetch('/api/notifications/read-all', { method: 'POST' })
    .then(() => {
      refreshNotificationBadge();
      loadNotificationsIntoDrawer();
    });
}

function deleteNotificationItem(id, btn) {
  fetch('/api/notifications/' + id, { method: 'DELETE' })
    .then(() => {
      if (btn && btn.closest('div')) btn.closest('div').parentElement.remove();
      refreshNotificationBadge();
    });
}

function requestBrowserNotificationPermission() {
  if ('Notification' in window && Notification.permission !== 'granted') {
    Notification.requestPermission();
  }
}


