/**
 * TUKAC Web Portal - Voice Assistant & Accessibility Suite
 * Provides screen reader capabilities, hover-to-read, keyboard shortcuts,
 * and visual aids (high contrast, text sizing) for students with disabilities.
 */

(function () {
  // Prevent duplicate initialization
  if (window.TukacVoiceAssistant) return;

  // --- STATE ---
  const state = {
    isOpen: false,
    isPlaying: false,
    isPaused: false,
    hoverToRead: false,
    highContrast: false,
    textSizeScale: 1.0, // 1.0, 1.15, 1.3, 1.5
    rate: 1.0, // Speech rate: 0.8, 1.0, 1.25, 1.5, 2.0
    fontFamily: 'default', // 'default', 'serif', 'mono', 'dyslexic'
    boldness: 'default', // 'default', 'medium', 'bold', 'extrabold'
    currentElementIndex: -1,
    readableElements: [],
    utterance: null,
    highlightClass: 'tukac-reading-highlight'
  };

  // --- CONFIG ---
  const SELECTORS = 'h1, h2, h3, h4, h5, h6, p, li, a, button, label, th, td, blockquote';
  
  // --- INITIALIZE ---
  function init() {
    injectStyles();
    createWidgetDOM();
    bindEvents();
    setupSpeechSynthesis();
    announceToScreenReader("Voice assistant and accessibility controls loaded. Press Control Shift V to open the menu.");
    
    // Check saved accessibility preferences
    loadPreferences();
  }

  // --- INJECT STYLES ---
  function injectStyles() {
    const styleEl = document.createElement('style');
    styleEl.id = 'tukac-accessibility-styles';
    styleEl.textContent = `
      @import url('https://cdn.jsdelivr.net/npm/opendyslexic@1.0.3/dist/opendyslexic.css');

      /* Accessibility Highlight */
      .tukac-reading-highlight {
        outline: 3px solid var(--gold, #C8921A) !important;
        outline-offset: 4px !important;
        background-color: rgba(200, 146, 26, 0.15) !important;
        border-radius: 4px !important;
        transition: outline 0.15s ease-in-out, background-color 0.15s ease-in-out !important;
      }

      /* Accessibility Control Widget */
      .tukac-voice-widget {
        position: fixed;
        bottom: 24px;
        right: 24px;
        z-index: 99999;
        font-family: 'Inter', system-ui, -apple-system, sans-serif;
      }
      
      /* Trigger Button */
      .tukac-voice-trigger {
        width: 60px;
        height: 60px;
        border-radius: 50%;
        background: linear-gradient(135deg, var(--navy, #003366), var(--navy-dark, #002244));
        color: #fff;
        border: 2px solid var(--gold, #C8921A);
        box-shadow: 0 8px 32px rgba(0, 51, 102, 0.35);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 26px;
        cursor: pointer;
        transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
        position: relative;
      }
      
      .tukac-voice-trigger:hover {
        transform: scale(1.08) translateY(-2px);
        box-shadow: 0 12px 36px rgba(0, 51, 102, 0.45);
        background: linear-gradient(135deg, var(--gold, #C8921A), var(--gold-light, #E5A820));
      }

      .tukac-voice-trigger .pulse-ring {
        position: absolute;
        border: 2px solid var(--gold, #C8921A);
        border-radius: 50%;
        animation: tukac-pulse 2s infinite;
        inset: -4px;
        opacity: 0;
        pointer-events: none;
      }
      
      .tukac-voice-trigger.active .pulse-ring {
        opacity: 1;
      }

      @keyframes tukac-pulse {
        0% { transform: scale(0.95); opacity: 0.8; }
        100% { transform: scale(1.3); opacity: 0; }
      }

      /* Control Panel */
      .tukac-voice-panel {
        position: absolute;
        bottom: 76px;
        right: 0;
        width: 330px;
        background: rgba(255, 255, 255, 0.9);
        backdrop-filter: blur(16px);
        -webkit-backdrop-filter: blur(16px);
        border: 1px solid rgba(255, 255, 255, 0.5);
        box-shadow: 0 16px 48px rgba(0, 34, 68, 0.25);
        border-radius: 16px;
        padding: 20px;
        display: none;
        flex-direction: column;
        gap: 16px;
        transform: translateY(10px);
        opacity: 0;
        transition: transform 0.25s ease, opacity 0.25s ease;
        border-top: 4px solid var(--gold, #C8921A);
        max-height: 80vh;
        overflow-y: auto;
      }

      .tukac-voice-panel.open {
        display: flex;
        transform: translateY(0);
        opacity: 1;
      }

      /* Header */
      .tukac-panel-header {
        display: flex;
        justify-content: space-between;
        align-items: center;
        border-bottom: 1px solid rgba(0, 0, 0, 0.08);
        padding-bottom: 10px;
      }

      .tukac-panel-title {
        font-weight: 800;
        font-size: 15px;
        color: var(--navy, #003366);
        display: flex;
        align-items: center;
        gap: 8px;
      }

      .tukac-panel-close {
        background: none;
        border: none;
        font-size: 18px;
        color: #64748b;
        cursor: pointer;
        padding: 4px;
        border-radius: 6px;
        display: flex;
        align-items: center;
        justify-content: center;
      }

      .tukac-panel-close:hover {
        background: rgba(0, 0, 0, 0.05);
        color: var(--danger, #dc2626);
      }

      /* Settings & Buttons Grid */
      .tukac-panel-section {
        display: flex;
        flex-direction: column;
        gap: 8px;
      }

      .tukac-section-label {
        font-size: 11px;
        font-weight: 700;
        color: #64748b;
        text-transform: uppercase;
        letter-spacing: 0.05em;
      }

      /* Control Buttons */
      .tukac-controls-row {
        display: flex;
        gap: 10px;
      }

      .tukac-control-btn {
        flex: 1;
        padding: 10px;
        border: none;
        border-radius: 8px;
        font-weight: 700;
        font-size: 13px;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 6px;
        cursor: pointer;
        transition: all 0.2s ease;
      }

      .tukac-btn-play {
        background: var(--navy, #003366);
        color: white;
      }

      .tukac-btn-play:hover {
        background: var(--navy-light, #1A5285);
      }

      .tukac-btn-stop {
        background: rgba(100, 116, 139, 0.1);
        color: #334155;
      }

      .tukac-btn-stop:hover {
        background: rgba(100, 116, 139, 0.2);
      }

      /* Toggle Switches */
      .tukac-toggle-item {
        display: flex;
        align-items: center;
        justify-content: space-between;
        padding: 8px 12px;
        background: rgba(0, 0, 0, 0.02);
        border-radius: 8px;
        border: 1px solid rgba(0, 0, 0, 0.04);
      }

      .tukac-toggle-info {
        display: flex;
        flex-direction: column;
      }

      .tukac-toggle-title {
        font-size: 13px;
        font-weight: 600;
        color: #1e293b;
      }

      .tukac-toggle-desc {
        font-size: 10px;
        color: #64748b;
      }

      /* Standard HTML Toggle Switch style */
      .tukac-switch {
        position: relative;
        display: inline-block;
        width: 44px;
        height: 24px;
        cursor: pointer;
      }

      .tukac-switch input {
        opacity: 0;
        width: 0;
        height: 0;
      }

      .tukac-slider {
        position: absolute;
        cursor: pointer;
        top: 0; left: 0; right: 0; bottom: 0;
        background-color: #cbd5e1;
        transition: .3s;
        border-radius: 24px;
      }

      .tukac-slider:before {
        position: absolute;
        content: "";
        height: 18px;
        width: 18px;
        left: 3px;
        bottom: 3px;
        background-color: white;
        transition: .3s;
        border-radius: 50%;
      }

      input:checked + .tukac-slider {
        background-color: var(--green, #1A6B3C);
      }

      input:checked + .tukac-slider:before {
        transform: translateX(20px);
      }

      /* Slider Controls */
      .tukac-slider-container {
        display: flex;
        flex-direction: column;
        gap: 6px;
      }
      
      .tukac-slider-header {
        display: flex;
        justify-content: space-between;
        font-size: 12px;
        color: #334155;
        font-weight: 500;
      }

      .tukac-range-input {
        width: 100%;
        accent-color: var(--navy, #003366);
        cursor: pointer;
      }

      /* Select Controls */
      .tukac-select-control {
        width: 100%;
        padding: 8px 12px;
        border: 1px solid rgba(0, 0, 0, 0.1);
        background: #fff;
        border-radius: 8px;
        font-size: 13px;
        font-weight: 500;
        color: #334155;
        outline: none;
        transition: all 0.2s ease;
        cursor: pointer;
      }
      .tukac-select-control:focus {
        border-color: var(--navy, #003366);
        box-shadow: 0 0 0 3px rgba(0, 51, 102, 0.12);
      }

      /* Visual Aid Options */
      .tukac-visual-grid {
        display: grid;
        grid-template-columns: 1fr 1fr;
        gap: 8px;
      }

      .tukac-visual-btn {
        padding: 8px;
        border: 1px solid rgba(0, 0, 0, 0.1);
        background: #fff;
        border-radius: 8px;
        font-size: 12px;
        font-weight: 600;
        color: #334155;
        cursor: pointer;
        display: flex;
        align-items: center;
        justify-content: center;
        gap: 6px;
        transition: all 0.2s ease;
      }

      .tukac-visual-btn:hover {
        background: rgba(0, 51, 102, 0.05);
        border-color: var(--navy, #003366);
      }

      .tukac-visual-btn.active {
        background: var(--navy, #003366);
        color: #fff;
        border-color: var(--navy, #003366);
      }

      /* Keyboard Shortcuts Reference */
      .tukac-shortcuts-card {
        background: rgba(200, 146, 26, 0.08);
        border: 1px solid rgba(200, 146, 26, 0.2);
        border-radius: 8px;
        padding: 10px;
        font-size: 11px;
        line-height: 1.5;
        color: #78350f;
      }

      .tukac-shortcuts-card strong {
        color: var(--gold, #C8921A);
      }

      /* Screen Reader Live Region (Visually Hidden) */
      .tukac-sr-only {
        position: absolute;
        width: 1px;
        height: 1px;
        padding: 0;
        margin: -1px;
        overflow: hidden;
        clip: rect(0, 0, 0, 0);
        white-space: nowrap;
        border: 0;
      }

      /* =========================================
         FONT & BOLDNESS INTERFACE RULES
         ========================================= */
      body.tukac-font-dyslexic * { font-family: 'OpenDyslexic', 'Comic Sans MS', sans-serif !important; }
      body.tukac-font-serif * { font-family: Georgia, 'Times New Roman', serif !important; }
      body.tukac-font-mono * { font-family: Consolas, 'Courier New', monospace !important; }

      body.tukac-weight-medium p,
      body.tukac-weight-medium li,
      body.tukac-weight-medium a,
      body.tukac-weight-medium span,
      body.tukac-weight-medium td,
      body.tukac-weight-medium th,
      body.tukac-weight-medium label,
      body.tukac-weight-medium input,
      body.tukac-weight-medium select,
      body.tukac-weight-medium button {
        font-weight: 500 !important;
      }
      body.tukac-weight-medium h1,
      body.tukac-weight-medium h2,
      body.tukac-weight-medium h3,
      body.tukac-weight-medium h4,
      body.tukac-weight-medium h5,
      body.tukac-weight-medium h6,
      body.tukac-weight-medium strong,
      body.tukac-weight-medium th {
        font-weight: 700 !important;
      }

      body.tukac-weight-bold p,
      body.tukac-weight-bold li,
      body.tukac-weight-bold a,
      body.tukac-weight-bold span,
      body.tukac-weight-bold td,
      body.tukac-weight-bold th,
      body.tukac-weight-bold label,
      body.tukac-weight-bold input,
      body.tukac-weight-bold select,
      body.tukac-weight-bold button {
        font-weight: 700 !important;
      }
      body.tukac-weight-bold h1,
      body.tukac-weight-bold h2,
      body.tukac-weight-bold h3,
      body.tukac-weight-bold h4,
      body.tukac-weight-bold h5,
      body.tukac-weight-bold h6,
      body.tukac-weight-bold strong,
      body.tukac-weight-bold th {
        font-weight: 800 !important;
      }

      body.tukac-weight-extrabold p,
      body.tukac-weight-extrabold li,
      body.tukac-weight-extrabold a,
      body.tukac-weight-extrabold span,
      body.tukac-weight-extrabold td,
      body.tukac-weight-extrabold th,
      body.tukac-weight-extrabold label,
      body.tukac-weight-extrabold input,
      body.tukac-weight-extrabold select,
      body.tukac-weight-extrabold button {
        font-weight: 800 !important;
      }
      body.tukac-weight-extrabold h1,
      body.tukac-weight-extrabold h2,
      body.tukac-weight-extrabold h3,
      body.tukac-weight-extrabold h4,
      body.tukac-weight-extrabold h5,
      body.tukac-weight-extrabold h6,
      body.tukac-weight-extrabold strong,
      body.tukac-weight-extrabold th {
        font-weight: 900 !important;
      }

      /* =========================================
         HIGH CONTRAST MODE CSS OVERRIDES
         ========================================= */
      body.tukac-high-contrast {
        background: #000000 !important;
        color: #ffffff !important;
      }

      body.tukac-high-contrast * {
        background-color: transparent !important;
        color: #ffffff !important;
        border-color: #ffffff !important;
        box-shadow: none !important;
        text-shadow: none !important;
      }

      body.tukac-high-contrast a,
      body.tukac-high-contrast a * {
        color: #ffff00 !important;
        text-decoration: underline !important;
      }

      body.tukac-high-contrast button,
      body.tukac-high-contrast input,
      body.tukac-high-contrast select,
      body.tukac-high-contrast textarea {
        background-color: #000000 !important;
        color: #ffffff !important;
        border: 2px solid #ffffff !important;
      }

      body.tukac-high-contrast button:hover,
      body.tukac-high-contrast input:focus {
        background-color: #ffff00 !important;
        color: #000000 !important;
      }

      body.tukac-high-contrast .badge {
        background-color: #000000 !important;
        border: 1.5px solid #ffffff !important;
      }

      /* Keep the accessibility widget visible with style overrides in high contrast */
      body.tukac-high-contrast .tukac-voice-widget,
      body.tukac-high-contrast .tukac-voice-widget * {
        background-color: #000000 !important;
        color: #ffffff !important;
        border-color: #ffffff !important;
      }

      body.tukac-high-contrast .tukac-voice-trigger {
        background: #000000 !important;
        border: 3px solid #ffff00 !important;
      }

      body.tukac-high-contrast .tukac-voice-trigger span {
        color: #ffff00 !important;
      }

      body.tukac-high-contrast .tukac-voice-panel {
        border: 3px solid #ffffff !important;
      }

      body.tukac-high-contrast .tukac-control-btn.tukac-btn-play {
        background: #ffff00 !important;
        color: #000000 !important;
        font-weight: 900 !important;
      }

      body.tukac-high-contrast .tukac-control-btn.tukac-btn-play * {
        color: #000000 !important;
      }

      body.tukac-high-contrast .tukac-visual-btn.active {
        background: #ffff00 !important;
        color: #000000 !important;
      }
      
      body.tukac-high-contrast .tukac-visual-btn.active * {
        color: #000000 !important;
      }

      body.tukac-high-contrast .tukac-reading-highlight {
        outline: 5px solid #ffff00 !important;
        background-color: rgba(255, 255, 0, 0.25) !important;
      }
      
      /* Mobile Adaptation */
      @media (max-width: 500px) {
        .tukac-voice-panel {
          width: calc(100vw - 48px);
          right: -8px;
        }
      }
    `;
    document.head.appendChild(styleEl);
  }

  // --- CREATE WIDGET DOM ---
  function createWidgetDOM() {
    const widget = document.createElement('div');
    widget.className = 'tukac-voice-widget';
    widget.setAttribute('role', 'region');
    widget.setAttribute('aria-label', 'Accessibility Controls');

    widget.innerHTML = `
      <!-- Trigger Button -->
      <button class="tukac-voice-trigger" id="tukacVoiceTrigger" aria-haspopup="true" aria-expanded="false" aria-label="Toggle voice assistant and accessibility menu">
        <span>🔊</span>
        <div class="pulse-ring"></div>
      </button>

      <!-- Panel -->
      <div class="tukac-voice-panel" id="tukacVoicePanel" aria-hidden="true">
        <!-- Header -->
        <div class="tukac-panel-header">
          <div class="tukac-panel-title">
            <span>♿</span> TUKAC Accessibility Assistant
          </div>
          <button class="tukac-panel-close" id="tukacPanelClose" aria-label="Close accessibility menu">✕</button>
        </div>

        <!-- Speech Controls -->
        <div class="tukac-panel-section">
          <div class="tukac-section-label">Screen Reader</div>
          <div class="tukac-controls-row">
            <button class="tukac-control-btn tukac-btn-play" id="tukacBtnPlay" aria-label="Start reading page aloud">
              <span id="tukacPlayIcon">▶️</span> <span id="tukacPlayText">Read Page</span>
            </button>
            <button class="tukac-control-btn tukac-btn-stop" id="tukacBtnStop" aria-label="Stop reading">
              <span>⏹️</span> Stop
            </button>
          </div>
        </div>

        <!-- Toggles Section -->
        <div class="tukac-panel-section">
          <div class="tukac-toggle-item">
            <div class="tukac-toggle-info">
              <div class="tukac-toggle-title">Hover to Read</div>
              <div class="tukac-toggle-desc">Hover mouse over text to read it</div>
            </div>
            <label class="tukac-switch">
              <input type="checkbox" id="tukacToggleHover">
              <span class="tukac-slider"></span>
            </label>
          </div>
        </div>

        <!-- Voice Settings -->
        <div class="tukac-panel-section">
          <div class="tukac-slider-container">
            <div class="tukac-slider-header">
              <span>Speech Rate</span>
              <span id="tukacRateVal">1.0x</span>
            </div>
            <input type="range" class="tukac-range-input" id="tukacRateRange" min="0.6" max="2.0" step="0.1" value="1.0" aria-label="Speech Speed">
          </div>
        </div>

        <!-- Font Selection -->
        <div class="tukac-panel-section">
          <div class="tukac-slider-header">
            <span>Font Style</span>
          </div>
          <select class="tukac-select-control" id="tukacSelectFont" aria-label="Select Font Style">
            <option value="default">Default Font</option>
            <option value="serif">Serif (Classic)</option>
            <option value="mono">Monospace (Code)</option>
            <option value="dyslexic">Dyslexia Friendly</option>
          </select>
        </div>

        <!-- Boldness Selection -->
        <div class="tukac-panel-section">
          <div class="tukac-slider-header">
            <span>Text Boldness</span>
          </div>
          <select class="tukac-select-control" id="tukacSelectWeight" aria-label="Select Text Boldness">
            <option value="default">Default Weight</option>
            <option value="medium">Medium Boldness</option>
            <option value="bold">Strong Bold</option>
            <option value="extrabold">Extra Heavy Bold</option>
          </select>
        </div>

        <!-- Visual Aids -->
        <div class="tukac-panel-section">
          <div class="tukac-section-label">Visual Enhancements</div>
          <div class="tukac-visual-grid">
            <button class="tukac-visual-btn" id="tukacBtnContrast" aria-label="Toggle High Contrast Mode">
              🌓 Contrast
            </button>
            <button class="tukac-visual-btn" id="tukacBtnTextSize" aria-label="Increase Text Size">
              🔎 Text Size
            </button>
          </div>
        </div>

        <!-- Keyboard Shortcuts Help -->
        <div class="tukac-shortcuts-card">
          <strong>Shortcuts:</strong> Alt + P (Play/Pause), Alt + S (Stop), Alt + H (Hover), Ctrl+Shift+V (Menu)
        </div>
      </div>

      <!-- ARIA Live Announcement Area -->
      <div id="tukacSrAnnouncement" class="tukac-sr-only" aria-live="polite" aria-atomic="true"></div>
    `;

    document.body.appendChild(widget);
  }

  // --- BIND EVENT LISTENERS ---
  function bindEvents() {
    const trigger = document.getElementById('tukacVoiceTrigger');
    const panel = document.getElementById('tukacVoicePanel');
    const closeBtn = document.getElementById('tukacPanelClose');
    const playBtn = document.getElementById('tukacBtnPlay');
    const stopBtn = document.getElementById('tukacBtnStop');
    const hoverToggle = document.getElementById('tukacToggleHover');
    const rateRange = document.getElementById('tukacRateRange');
    const contrastBtn = document.getElementById('tukacBtnContrast');
    const textSizeBtn = document.getElementById('tukacBtnTextSize');
    const fontSelect = document.getElementById('tukacSelectFont');
    const weightSelect = document.getElementById('tukacSelectWeight');

    // Panel Toggle
    trigger.addEventListener('click', togglePanel);
    closeBtn.addEventListener('click', closePanel);

    // Audio Controls
    playBtn.addEventListener('click', handlePlayPause);
    stopBtn.addEventListener('click', handleStop);

    // Rate Slider
    rateRange.addEventListener('input', (e) => {
      const rateVal = parseFloat(e.target.value);
      state.rate = rateVal;
      document.getElementById('tukacRateVal').textContent = rateVal.toFixed(1) + 'x';
      
      // If reading, restart current block with new rate
      if (state.isPlaying && !state.isPaused) {
        speakCurrentElement();
      }
    });

    // Hover Toggle
    hoverToggle.addEventListener('change', (e) => {
      state.hoverToRead = e.target.checked;
      savePreference('hoverToRead', state.hoverToRead);
      announceToScreenReader(state.hoverToRead ? "Hover to read activated" : "Hover to read deactivated");
      if (state.hoverToRead) {
        handleStop(); // Stop automatic full-page reader
      }
    });

    // Visual Enhancements
    contrastBtn.addEventListener('click', toggleHighContrast);
    textSizeBtn.addEventListener('click', cycleTextScale);

    // Font Style & Boldness Dropdowns
    fontSelect.addEventListener('change', (e) => {
      applyFontFamily(e.target.value);
      savePreference('font', e.target.value);
    });

    weightSelect.addEventListener('change', (e) => {
      applyBoldness(e.target.value);
      savePreference('weight', e.target.value);
    });

    // Keyboard Shortcuts
    document.addEventListener('keydown', (e) => {
      // Ctrl + Shift + V -> Open/Close Widget
      if (e.ctrlKey && e.shiftKey && e.code === 'KeyV') {
        e.preventDefault();
        togglePanel();
      }
      // Alt + P -> Play/Pause
      else if (e.altKey && e.code === 'KeyP') {
        e.preventDefault();
        handlePlayPause();
      }
      // Alt + S -> Stop
      else if (e.altKey && e.code === 'KeyS') {
        e.preventDefault();
        handleStop();
      }
      // Alt + H -> Hover Toggle
      else if (e.altKey && e.code === 'KeyH') {
        e.preventDefault();
        const hoverToggleEl = document.getElementById('tukacToggleHover');
        if (hoverToggleEl) hoverToggleEl.click();
      }
      // Alt + ArrowDown -> Read next element
      else if (e.altKey && e.code === 'ArrowDown') {
        e.preventDefault();
        readNextElement();
      }
      // Alt + ArrowUp -> Read previous element
      else if (e.altKey && e.code === 'ArrowUp') {
        e.preventDefault();
        readPrevElement();
      }
      // Alt + Enter -> Click/activate current element
      else if (e.altKey && e.code === 'Enter') {
        const activeTag = document.activeElement ? document.activeElement.tagName.toLowerCase() : '';
        // Only trigger Alt + Enter if we aren't already focusing and hitting Enter on an input or button naturally
        if (activeTag !== 'input' && activeTag !== 'button' && activeTag !== 'select') {
          e.preventDefault();
          clickCurrentElement();
        }
      }
    });

    // Mouseover Delegation for Hover-to-Read
    document.addEventListener('mouseover', handleMouseOver);
    document.addEventListener('mouseout', handleMouseOut);

    // Focus listener to automatically read elements tabbed into
    document.addEventListener('focus', (e) => {
      const target = e.target;
      if (!target) return;
      
      // Skip reading widget controls on tab focus unless panel is open
      if (target.closest('.tukac-voice-widget') && !state.isOpen) return;

      const tag = target.tagName.toLowerCase();
      const isReadable = ['a', 'button', 'input', 'select', 'textarea'].includes(tag) || target.getAttribute('tabindex') !== null;
      if (!isReadable) return;

      let text = target.innerText || target.value || target.placeholder || '';
      if (!text && target.getAttribute('aria-label')) {
        text = target.getAttribute('aria-label');
      }
      text = text.trim();
      if (!text) return;

      let cue = "";
      if (tag === 'a') cue = "Link: ";
      else if (tag === 'button') cue = "Button: ";
      else if (tag === 'input') cue = "Input field: " + (target.placeholder || "");
      else if (tag === 'select') cue = "Dropdown selection: ";

      window.speechSynthesis.cancel();
      const utt = new SpeechSynthesisUtterance(cue + text);
      utt.rate = state.rate;
      
      const voices = window.speechSynthesis.getVoices();
      const englishVoice = voices.find(voice => voice.lang.startsWith('en'));
      if (englishVoice) utt.voice = englishVoice;

      window.speechSynthesis.speak(utt);
      
      // Sync element highlight pointer
      if (state.readableElements.length === 0) {
        scanReadableElements();
      }
      const index = state.readableElements.indexOf(target);
      if (index !== -1) {
        state.currentElementIndex = index;
        highlightElement(target);
      }
    }, true);
  }

  // --- PREFERENCES STORAGE ---
  function loadPreferences() {
    const savedContrast = localStorage.getItem('tukac_contrast') === 'true';
    const savedHover = localStorage.getItem('tukac_hover') === 'true';
    const savedScale = parseFloat(localStorage.getItem('tukac_text_scale') || '1.0');
    const savedRate = parseFloat(localStorage.getItem('tukac_speech_rate') || '1.0');
    const savedFont = localStorage.getItem('tukac_font') || 'default';
    const savedWeight = localStorage.getItem('tukac_weight') || 'default';

    if (savedContrast) {
      toggleHighContrast(null, true);
    }
    if (savedHover) {
      document.getElementById('tukacToggleHover').checked = true;
      state.hoverToRead = true;
    }
    if (savedScale !== 1.0) {
      applyTextScale(savedScale);
    }
    if (savedRate !== 1.0) {
      state.rate = savedRate;
      document.getElementById('tukacRateRange').value = savedRate;
      document.getElementById('tukacRateVal').textContent = savedRate.toFixed(1) + 'x';
    }
    if (savedFont !== 'default') {
      document.getElementById('tukacSelectFont').value = savedFont;
      applyFontFamily(savedFont);
    }
    if (savedWeight !== 'default') {
      document.getElementById('tukacSelectWeight').value = savedWeight;
      applyBoldness(savedWeight);
    }
  }

  function savePreference(key, val) {
    localStorage.setItem('tukac_' + key, val);
  }

  // --- SPEECH ENGINE FUNCTIONS ---
  function setupSpeechSynthesis() {
    // Stop speaking if window is closed or refreshed
    window.addEventListener('beforeunload', () => {
      window.speechSynthesis.cancel();
    });
  }

  function handlePlayPause() {
    if (state.hoverToRead) {
      // Disable hover-to-read first when reading the whole page
      document.getElementById('tukacToggleHover').checked = false;
      state.hoverToRead = false;
      savePreference('hoverToRead', false);
    }

    if (state.isPlaying) {
      if (state.isPaused) {
        // Resume
        window.speechSynthesis.resume();
        state.isPaused = false;
        setPlayState(true);
        announceToScreenReader("Resuming screen reader");
      } else {
        // Pause
        window.speechSynthesis.pause();
        state.isPaused = true;
        setPlayState(false);
        announceToScreenReader("Reading paused");
      }
    } else {
      // Start fresh
      startReading();
    }
  }

  function handleStop() {
    window.speechSynthesis.cancel();
    removeHighlight();
    state.isPlaying = false;
    state.isPaused = false;
    state.currentElementIndex = -1;
    setPlayState(false);
    announceToScreenReader("Reading stopped");
  }

  function setPlayState(playing) {
    const playIcon = document.getElementById('tukacPlayIcon');
    const playText = document.getElementById('tukacPlayText');
    const playBtn = document.getElementById('tukacBtnPlay');
    const trigger = document.getElementById('tukacVoiceTrigger');

    if (playing) {
      playIcon.textContent = '⏸️';
      playText.textContent = 'Pause';
      playBtn.setAttribute('aria-label', 'Pause reading');
      trigger.classList.add('active');
    } else {
      playIcon.textContent = '▶️';
      playText.textContent = state.isPaused ? 'Resume' : 'Read Page';
      playBtn.setAttribute('aria-label', state.isPaused ? 'Resume reading page aloud' : 'Start reading page aloud');
      if (!state.isPaused) {
        trigger.classList.remove('active');
      }
    }
  }

  function startReading() {
    // Scan DOM for readable elements
    scanReadableElements();

    if (state.readableElements.length === 0) {
      announceToScreenReader("No readable text found on this page.");
      return;
    }

    state.isPlaying = true;
    state.isPaused = false;
    state.currentElementIndex = 0;
    setPlayState(true);
    announceToScreenReader("Starting screen reader. Reading " + state.readableElements.length + " elements.");
    speakCurrentElement();
  }

  function scanReadableElements() {
    const all = Array.from(document.querySelectorAll(SELECTORS));
    
    // Filter elements to read
    state.readableElements = all.filter(el => {
      // Check visibility
      if (el.offsetWidth === 0 && el.offsetHeight === 0) return false;
      const style = window.getComputedStyle(el);
      if (style.display === 'none' || style.visibility === 'hidden' || style.opacity === '0') return false;

      // Filter elements in the accessibility widget itself
      if (el.closest('.tukac-voice-widget')) return false;

      // Filter empty nodes
      const text = el.innerText || el.textContent;
      if (!text || text.trim().length === 0) return false;

      // Filter utility / header-nav items to avoid announcement spam
      if (el.closest('.sidebar-header') || el.closest('.sidebar-footer') || el.closest('.topbar-left button')) {
        // Let's only read main layout content, or allow if explicitly reading sidebar links
        // We can just skip icons or duplicate items
      }

      return true;
    });
  }

  function speakCurrentElement() {
    window.speechSynthesis.cancel(); // Cancel current audio
    
    if (state.currentElementIndex < 0 || state.currentElementIndex >= state.readableElements.length) {
      handleStop();
      announceToScreenReader("Finished reading page content.");
      return;
    }

    const element = state.readableElements[state.currentElementIndex];
    highlightElement(element);
    element.scrollIntoView({ behavior: 'smooth', block: 'center' });

    let rawText = element.innerText || element.textContent || '';
    let speechText = rawText.trim();
    
    // Add audio context cues for accessibility
    const tag = element.tagName.toLowerCase();
    if (tag.startsWith('h') && tag.length === 2) {
      const level = tag.charAt(1);
      speechText = `Heading ${level}: ` + speechText;
    } else if (tag === 'a') {
      speechText = "Link: " + speechText;
    } else if (tag === 'button') {
      speechText = "Button: " + speechText;
    } else if (tag === 'blockquote') {
      speechText = "Quote: " + speechText;
    }

    state.utterance = new SpeechSynthesisUtterance(speechText);
    state.utterance.rate = state.rate;
    
    // Select an English voice if available
    const voices = window.speechSynthesis.getVoices();
    const englishVoice = voices.find(voice => voice.lang.startsWith('en') && voice.name.includes('Natural')) || 
                        voices.find(voice => voice.lang.startsWith('en'));
    if (englishVoice) {
      state.utterance.voice = englishVoice;
    }

    state.utterance.onend = () => {
      // Prevent running if we stopped or paused in between
      if (!state.isPlaying || state.isPaused) return;

      state.currentElementIndex++;
      speakCurrentElement();
    };

    state.utterance.onerror = (e) => {
      console.error("Speech error", e);
      if (e.error !== 'interrupted') {
        state.currentElementIndex++;
        speakCurrentElement();
      }
    };

    window.speechSynthesis.speak(state.utterance);
  }

  function highlightElement(el) {
    removeHighlight();
    el.classList.add(state.highlightClass);
  }

  function removeHighlight() {
    document.querySelectorAll('.' + state.highlightClass).forEach(el => {
      el.classList.remove(state.highlightClass);
    });
  }

  // --- HOVER TO READ ---
  let hoverTimeout = null;
  let lastHoveredElement = null;

  function handleMouseOver(e) {
    if (!state.hoverToRead) return;
    
    const target = e.target.closest(SELECTORS);
    if (!target) return;
    if (target.closest('.tukac-voice-widget')) return;
    if (target === lastHoveredElement) return;

    lastHoveredElement = target;
    
    // Clear hover highlight/timeout from previous
    clearTimeout(hoverTimeout);
    removeHighlight();

    // Small delay to prevent voice spamming while sweeping cursor across the page
    hoverTimeout = setTimeout(() => {
      highlightElement(target);
      
      let rawText = target.innerText || target.textContent || '';
      let speechText = rawText.trim();
      const tag = target.tagName.toLowerCase();
      if (tag.startsWith('h') && tag.length === 2) {
        speechText = `Heading: ${speechText}`;
      } else if (tag === 'a') {
        speechText = `Link: ${speechText}`;
      } else if (tag === 'button') {
        speechText = `Button: ${speechText}`;
      }

      window.speechSynthesis.cancel();
      const utt = new SpeechSynthesisUtterance(speechText);
      utt.rate = state.rate;
      
      const voices = window.speechSynthesis.getVoices();
      const englishVoice = voices.find(voice => voice.lang.startsWith('en')) ;
      if (englishVoice) utt.voice = englishVoice;

      window.speechSynthesis.speak(utt);
    }, 250);
  }

  function handleMouseOut(e) {
    if (!state.hoverToRead) return;
    const target = e.target.closest(SELECTORS);
    if (!target) return;

    if (target === lastHoveredElement) {
      clearTimeout(hoverTimeout);
      window.speechSynthesis.cancel();
      removeHighlight();
      lastHoveredElement = null;
    }
  }

  // --- VISUAL ENHANCEMENTS ---
  function toggleHighContrast(e, force = false) {
    state.highContrast = typeof force === 'boolean' ? force : !state.highContrast;
    
    const body = document.body;
    const contrastBtn = document.getElementById('tukacBtnContrast');
    
    if (state.highContrast) {
      body.classList.add('tukac-high-contrast');
      contrastBtn.classList.add('active');
      announceToScreenReader("High contrast mode turned on");
    } else {
      body.classList.remove('tukac-high-contrast');
      contrastBtn.classList.remove('active');
      announceToScreenReader("High contrast mode turned off");
    }
    
    savePreference('contrast', state.highContrast);
  }

  function cycleTextScale() {
    let nextScale = 1.0;
    if (state.textSizeScale === 1.0) nextScale = 1.15;
    else if (state.textSizeScale === 1.15) nextScale = 1.3;
    else if (state.textSizeScale === 1.3) nextScale = 1.5;
    else nextScale = 1.0;

    applyTextScale(nextScale);
    savePreference('text_scale', nextScale);
  }

  function applyTextScale(scale) {
    state.textSizeScale = scale;
    const root = document.documentElement;
    const scaleBtn = document.getElementById('tukacBtnTextSize');

    // Scale font size variables
    if (scale === 1.0) {
      root.style.removeProperty('--tukac-text-scale');
      document.body.style.removeProperty('font-size');
      scaleBtn.classList.remove('active');
      scaleBtn.innerHTML = '🔎 Text Size';
      announceToScreenReader("Text size set to normal");
    } else {
      root.style.setProperty('--tukac-text-scale', scale);
      // Directly scale base body font size so standard elements inherit correctly
      document.body.style.fontSize = `calc(14px * ${scale})`;
      scaleBtn.classList.add('active');
      scaleBtn.innerHTML = `🔎 Size: ${scale}x`;
      announceToScreenReader(`Text size increased to ${scale * 100} percent`);
    }
  }

  function applyFontFamily(font) {
    state.fontFamily = font;
    const body = document.body;
    body.classList.remove('tukac-font-dyslexic', 'tukac-font-serif', 'tukac-font-mono');
    
    if (font === 'dyslexic') {
      body.classList.add('tukac-font-dyslexic');
      announceToScreenReader("Dyslexia friendly font applied");
    } else if (font === 'serif') {
      body.classList.add('tukac-font-serif');
      announceToScreenReader("Serif font applied");
    } else if (font === 'mono') {
      body.classList.add('tukac-font-mono');
      announceToScreenReader("Monospace font applied");
    } else {
      announceToScreenReader("Default font applied");
    }
  }

  function applyBoldness(weight) {
    state.boldness = weight;
    const body = document.body;
    body.classList.remove('tukac-weight-medium', 'tukac-weight-bold', 'tukac-weight-extrabold');
    
    if (weight === 'medium') {
      body.classList.add('tukac-weight-medium');
      announceToScreenReader("Medium boldness applied");
    } else if (weight === 'bold') {
      body.classList.add('tukac-weight-bold');
      announceToScreenReader("Strong boldness applied");
    } else if (weight === 'extrabold') {
      body.classList.add('tukac-weight-extrabold');
      announceToScreenReader("Extra heavy boldness applied");
    } else {
      announceToScreenReader("Default boldness applied");
    }
  }

  // --- PANEL UI FUNCTIONS ---
  function togglePanel() {
    if (state.isOpen) {
      closePanel();
    } else {
      openPanel();
    }
  }

  function openPanel() {
    const trigger = document.getElementById('tukacVoiceTrigger');
    const panel = document.getElementById('tukacVoicePanel');
    
    state.isOpen = true;
    panel.classList.add('open');
    panel.setAttribute('aria-hidden', 'false');
    trigger.setAttribute('aria-expanded', 'true');
    
    announceToScreenReader("Voice assistant menu opened. Tab to navigate options.");
    
    // Focus the first button in the panel for accessibility
    setTimeout(() => {
      document.getElementById('tukacBtnPlay').focus();
    }, 150);
  }

  function closePanel() {
    const trigger = document.getElementById('tukacVoiceTrigger');
    const panel = document.getElementById('tukacVoicePanel');
    
    state.isOpen = false;
    panel.classList.remove('open');
    panel.setAttribute('aria-hidden', 'true');
    trigger.setAttribute('aria-expanded', 'false');
    
    announceToScreenReader("Voice assistant menu closed");
    trigger.focus();
  }

  // --- SCREEN READER ANNOUNCEMENTS (ARIA LIVE) ---
  function announceToScreenReader(message) {
    const liveRegion = document.getElementById('tukacSrAnnouncement');
    if (liveRegion) {
      liveRegion.textContent = '';
      setTimeout(() => {
        liveRegion.textContent = message;
      }, 50);
    }
  }

  // Run on load
  if (document.readyState === 'loading') {
    document.addEventListener('DOMContentLoaded', init);
  } else {
    init();
  }

  // Export globally for integrations
  window.TukacVoiceAssistant = {
    state,
    togglePanel,
    play: startReading,
    pause: handlePlayPause,
    stop: handleStop,
    toggleContrast: toggleHighContrast,
    setTextScale: applyTextScale,
    announce: announceToScreenReader
  };
})();
