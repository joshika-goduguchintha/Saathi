// =============================================================================
// Saathi – Multilingual Voice Livelihood Assistant (Frontend Controller)
// =============================================================================

// Global State
let allBeneficiaries = [];
let catalogData = { trainingPrograms: [], opportunities: [] };
let currentAppLang = 'en-IN'; // 'te-IN' | 'ta-IN' | 'hi-IN' | 'kn-IN' | 'en-IN'
let chatVoiceEnabled = true;
let currentModalBenId = null;

// =============================================================================
// UNIVERSAL I18N TRANSLATION DICTIONARY
// =============================================================================
const I18N = {
    'en-IN': {
        header_title: 'Officer Dashboard',
        header_subtitle: 'Training-to-Placement Pipeline & Real-Time Monitoring',
        btn_register_beneficiary: '+ Register Beneficiary',
        gateway_badge: '✨ Multi-Modal Beneficiary Intake',
        gateway_title: 'Select Onboarding Pathway',
        gateway_subtitle: 'Choose the intake method tailored for your beneficiary\'s digital accessibility level',
        pill_call: 'Interactive Voice Agent',
        card_call_title: 'Assisted Voice Call',
        card_call_desc: 'One-click simulated telephone call. The AI officer asks questions out loud in your native language with automatic turn-taking.',
        pill_ptt: 'Push-to-Talk Guided',
        card_ptt_title: 'Voice Note Registration',
        card_ptt_desc: 'Step-by-step intake with a large tactile microphone button. Hold or tap to speak your response with real-time feedback.',
        pill_form: 'Direct Manual Entry',
        card_form_title: 'Direct Form Entry',
        card_form_desc: 'Traditional structured registration form, fully translated into Telugu, Tamil, Hindi, Kannada, or English.',
        metric_total: 'Total Beneficiaries',
        metric_enrolled: 'Enrolled / Training',
        metric_placed: 'Placed / Self-Employed',
        metric_support: 'Needs Officer Support',
        hitl_title: 'HUMAN-IN-THE-LOOP ALERT TRIGGERED!',
        hitl_desc: 'There are beneficiaries marked \'Needs Officer Support\'. Please review their profiles below.',
        btn_filter_support: 'Filter Support Cases',
        table_pipeline_title: 'Beneficiary Pipeline Overview',
        btn_refresh: 'Refresh',
        th_id: 'ID',
        th_name: 'Name',
        th_phone: 'Phone',
        th_region: 'Region',
        th_status: 'Pipeline Status',
        th_date: 'Registration Date',
        th_action: 'Action',
        call_agent_name: 'Saathi AI Voice Officer',
        fallback_title: '⌨️ Type Answer Instead',
        btn_submit: 'Submit',
        ctrl_replay: 'Replay',
        ctrl_type: 'Type',
        ctrl_end_call: 'End Call',
        btn_close: 'Close',
        btn_view_roadmap: '🗺️ View Personalized Roadmap'
    },
    'te-IN': {
        header_title: 'అధికారి డ్యాష్‌బోర్డ్',
        header_subtitle: 'శిక్షణ నుండి ఉపాధి వరకు పైప్‌లైన్ & రియల్-టైమ్ పర్యవేక్షణ',
        btn_register_beneficiary: '+ లబ్ధిదారుని నమోదు చేయండి',
        gateway_badge: '✨ బహుళ-మాధ్యమ నమోదు మార్గాలు',
        gateway_title: 'నమోదు విధానాన్ని ఎంచుకోండి',
        gateway_subtitle: 'లబ్ధిదారునికి అనువైన సౌకర్యవంతమైన నమోదు విధానాన్ని ఎంచుకోండి',
        pill_call: 'ఇంటరాక్టివ్ వాయిస్ ఏజెంట్',
        card_call_title: 'వాయిస్ కాల్ సహాయం (AI కాల్)',
        card_call_desc: 'నేరుగా బ్రౌజర్‌లో వాయిస్ కాల్. AI అధికారి మీతో తెలుగులో ప్రశ్నలు అడుగుతూ వివరాలు సేకరిస్తారు.',
        pill_ptt: 'వాయిస్ నోట్ గైడెడ్',
        card_ptt_title: 'వాయిస్ నోట్ నమోదు',
        card_ptt_desc: 'పెద్ద మైక్రోఫోన్ బటన్ నొక్కి మాట్లాడి దశలవారీగా మీ వివరాలను సులభంగా నమోదు చేయండి.',
        pill_form: 'నేరుగా ఫారమ్ ఎంట్రీ',
        card_form_title: 'డైరెక్ట్ ఫారమ్ నమోదు',
        card_form_desc: 'సాంప్రదాయ నమోదు ఫారమ్, తెలుగు భాషలో స్పష్టమైన ఫీల్డ్ సూచనలతో.',
        metric_total: 'మొత్తం లబ్ధిదారులు',
        metric_enrolled: 'శిక్షణలో ఉన్నవారు',
        metric_placed: 'ఉపాధి పొందినవారు',
        metric_support: 'అధికారి సహాయం కావాల్సినవారు',
        hitl_title: 'మానవ సహాయ హెచ్చరిక!',
        hitl_desc: 'కొందరు లబ్ధిదారులకు అధికారి సహాయం అవసరం. దయచేసి వారి వివరాలను పరిశీలించండి.',
        btn_filter_support: 'సహాయ కేసులు చూపించు',
        table_pipeline_title: 'లబ్ధిదారుల పైప్‌లైన్ వివరాలు',
        btn_refresh: 'తాజాకరించు',
        th_id: 'ఐడి',
        th_name: 'పేరు',
        th_phone: 'ఫోన్ నంబర్',
        th_region: 'ప్రాంతం',
        th_status: 'ప్రస్తుత స్థితి',
        th_date: 'నమోదు తేదీ',
        th_action: 'చర్య',
        call_agent_name: 'సాథి AI వాయిస్ అధికారి',
        fallback_title: '⌨️ సమాధానం టైప్ చేయండి',
        btn_submit: 'సమర్పించు',
        ctrl_replay: 'మళ్ళీ విను',
        ctrl_type: 'టైప్ చేయి',
        ctrl_end_call: 'కాల్ ముగించు',
        btn_close: 'మూసివేయి',
        btn_view_roadmap: '🗺️ ఉపాధి రోడ్‌మ్యాప్ చూడండి'
    },
    'ta-IN': {
        header_title: 'அதிகாரி கட்டுப்பாட்டு பலகை',
        header_subtitle: 'பயிற்சி முதல் வேலைவாய்ப்பு வரை நிகழ்நேர கண்காணிப்பு',
        btn_register_beneficiary: '+ பயனாளியை பதிவு செய்க',
        gateway_badge: '✨ பல வழி பயனாளி சேர்க்கை',
        gateway_title: 'பதிவு செய்யும் முறையை தேர்வு செய்க',
        gateway_subtitle: 'பயனாளியின் வசதிக்கேற்ப ஏற்ற பதிவு முறையை தேர்ந்தெடுக்கவும்',
        pill_call: 'குரல் முகவர் அழைப்பு',
        card_call_title: 'AI நேரடி குரல் அழைப்பு',
        card_call_desc: 'ஒரே கிளிக்கில் நேரடி குரல் அழைப்பு. AI அதிகாரி தமிழில் கேள்விகள் கேட்டு விவரங்களை பதிவு செய்வார்.',
        pill_ptt: 'குரல் பதிவு முறை',
        card_ptt_title: 'குரல் குறிப்பு பதிவு',
        card_ptt_desc: 'மைக் பட்டனை அழுத்திப் பேசி படிப்படியாக விவரங்களை பதிவு செய்யும் முறை.',
        pill_form: 'நேரடி படிவம்',
        card_form_title: 'நேரடி படிவ பதிவு',
        card_form_desc: 'பாரம்பரிய பதிவு படிவம், முழுமையான தமிழ் மொழிபெயர்ப்புடன்.',
        metric_total: 'மொத்த பயனாளிகள்',
        metric_enrolled: 'பயிற்சியில் உள்ளோர்',
        metric_placed: 'வேலைவாய்ப்பு பெற்றோர்',
        metric_support: 'அதிகாரி உதவி தேவைப்படுவோர்',
        hitl_title: 'அதிகாரி உதவி எச்சரிக்கை!',
        hitl_desc: 'சில பயனாளிகளுக்கு அதிகாரி உதவி தேவைப்படுகிறது. தயவுசெய்து மதிப்பாய்வு செய்யவும்.',
        btn_filter_support: 'வழக்குகளை வடிகட்டு',
        table_pipeline_title: 'பயனாளி நிலை மேலோட்டம்',
        btn_refresh: 'புதுப்பி',
        th_id: 'எண்',
        th_name: 'பெயர்',
        th_phone: 'தொலைபேசி',
        th_region: 'மாவட்டம்',
        th_status: 'தற்போதைய நிலை',
        th_date: 'பதிவு தேதி',
        th_action: 'செயல்',
        call_agent_name: 'சாதி AI குரல் அதிகாரி',
        fallback_title: '⌨️ தட்டச்சு செய்க',
        btn_submit: 'சமர்ப்பி',
        ctrl_replay: 'மீண்டும் கேள்',
        ctrl_type: 'தட்டச்சு',
        ctrl_end_call: 'அழைப்பை முடி',
        btn_close: 'மூடு',
        btn_view_roadmap: '🗺️ வாழ்வாதார வரைபடம் பார்'
    },
    'hi-IN': {
        header_title: 'अधिकारी डैशबोर्ड',
        header_subtitle: 'प्रशिक्षण से रोजगार पाइपलाइन और रीयल-टाइम निगरानी',
        btn_register_beneficiary: '+ लाभार्थी पंजीकृत करें',
        gateway_badge: '✨ बहु-माध्यम लाभार्थी प्रवेश',
        gateway_title: 'पंजीकरण मार्ग चुनें',
        gateway_subtitle: 'लाभार्थी की डिजिटल सुविधा के अनुसार सही माध्यम चुनें',
        pill_call: 'इंटरएक्टिव वॉयस एजेंट',
        card_call_title: 'सहायता प्राप्त वॉयस कॉल',
        card_call_desc: 'एक क्लिक में AI वॉयस कॉल शुरू करें। AI अधिकारी हिंदी में प्रश्न पूछेगा और आपकी आवाज से प्रोफाइल बनाएगा।',
        pill_ptt: 'पुश-टू-टॉक गाइडेड',
        card_ptt_title: 'वॉयस नोट पंजीकरण',
        card_ptt_desc: 'माइक बटन दबाकर बोलें और चरण-दर-चरण अपनी प्रोफाइल आसानी से बनाएं।',
        pill_form: 'सीधा फॉर्म',
        card_form_title: 'डायरेक्ट फॉर्म पंजीकरण',
        card_form_desc: 'पारंपरिक पंजीकरण फॉर्म, संपूर्ण हिंदी अनुवाद और स्पष्ट मार्गदर्शन के साथ।',
        metric_total: 'कुल लाभार्थी',
        metric_enrolled: 'प्रशिक्षण में नामांकित',
        metric_placed: 'रोजगार / स्वरोजगार प्राप्त',
        metric_support: 'अधिकारी सहायता अपेक्षित',
        hitl_title: 'मानव सहायता चेतावनी!',
        hitl_desc: 'कुछ लाभार्थियों को अधिकारी सहायता की आवश्यकता है। कृपया समीक्षा करें।',
        btn_filter_support: 'सहायता मामले देखें',
        table_pipeline_title: 'लाभार्थी पाइपलाइन विवरण',
        btn_refresh: 'रिफ्रेश',
        th_id: 'आईडी',
        th_name: 'नाम',
        th_phone: 'फोन नंबर',
        th_region: 'क्षेत्र / जिला',
        th_status: 'पाइपलाइन स्थिति',
        th_date: 'पंजीकरण तिथि',
        th_action: 'कार्रवाई',
        call_agent_name: 'साथी AI वॉयस अधिकारी',
        fallback_title: '⌨️ लिखकर उत्तर दें',
        btn_submit: 'जमा करें',
        ctrl_replay: 'पुनः सुनें',
        ctrl_type: 'टाइप करें',
        ctrl_end_call: 'कॉल समाप्त करें',
        btn_close: 'बंद करें',
        btn_view_roadmap: '🗺️ आजीविका रोडमैप देखें'
    },
    'kn-IN': {
        header_title: 'ಅಧಿಕಾರಿ ಡ್ಯಾಶ್‌ಬೋರ್ಡ್',
        header_subtitle: 'ತರಬೇತಿಯಿಂದ ಉದ್ಯೋಗದವರೆಗೆ ನೈಜ ಸಮಯದ ಮೇಲ್ವಿಚಾರಣೆ',
        btn_register_beneficiary: '+ ಫಲಾನುಭವಿ ನೋಂದಣಿ',
        gateway_badge: '✨ ಬಹು-ಮಾದರಿ ಫಲಾನುಭವಿ ನೋಂದಣಿ',
        gateway_title: 'ನೋಂದಣಿ ಮಾರ್ಗವನ್ನು ಆಯ್ಕೆಮಾಡಿ',
        gateway_subtitle: 'ಫಲಾನುಭವಿಯ ಅನುಕೂಲಕ್ಕೆ ತಕ್ಕಂತೆ ನೋಂದಣಿ ವಿಧಾನವನ್ನು ಆಯ್ಕೆಮಾಡಿ',
        pill_call: 'ಇಂಟರ್ಯಾಕ್ಟಿವ್ ವಾಯ್ಸ್ ಏಜೆಂಟ್',
        card_call_title: 'AI ಧ್ವನಿ ಕರೆ ನೆರವು',
        card_call_desc: 'ಒಂದೇ ಕ್ಲಿಕ್‌ನಲ್ಲಿ ಧ್ವನಿ ಕರೆ. AI ಅಧಿಕಾರಿಯು ಕನ್ನಡದಲ್ಲಿ ಪ್ರಶ್ನೆಗಳನ್ನು ಕೇಳಿ ಮಾಹಿತಿ ದಾಖಲಿಸುತ್ತಾರೆ.',
        pill_ptt: 'ಪುಶ್-ಟು-ಟಾಕ್ ಮಾರ್ಗದರ್ಶಿ',
        card_ptt_title: 'ಧ್ವನಿ ಟಿಪ್ಪಣಿ ನೋಂದಣಿ',
        card_ptt_desc: 'ಮೈಕ್ ಬಟನ್ ಒತ್ತಿ ಮಾತನಾಡಿ ಹಂತ ಹಂತವಾಗಿ ಸುಲಭವಾಗಿ ನೋಂದಾಯಿಸಿ.',
        pill_form: 'ನೇರ ನಮೂನೆ',
        card_form_title: 'ನೇರ ನಮೂನೆ ನೋಂದಣಿ',
        card_form_desc: 'ಸಾಂಪ್ರದಾಯಿಕ ನೋಂದಣಿ ನಮೂನೆ, ಸಂಪೂರ್ಣ ಕನ್ನಡ ಅನುವಾದದೊಂದಿಗೆ.',
        metric_total: 'ಒಟ್ಟು ಫಲಾನುಭವಿಗಳು',
        metric_enrolled: 'ತರಬೇತಿಯಲ್ಲಿರುವವರು',
        metric_placed: 'ಉದ್ಯೋಗ ಪಡೆದವರು',
        metric_support: 'ಅಧಿಕಾರಿ ಬೆಂಬಲ ಅಗತ್ಯವಿರುವವರು',
        hitl_title: 'ಅಧಿಕಾರಿ ನೆರವು ಎಚ್ಚರಿಕೆ!',
        hitl_desc: 'ಕೆಲವು ಫಲಾನುಭವಿಗಳಿಗೆ ಅಧಿಕಾರಿ ನೆರವು ಅಗತ್ಯವಿದೆ. ದಯವಿಟ್ಟು ಪರಿಶೀಲಿಸಿ.',
        btn_filter_support: 'ನೆರವು ಪ್ರಕರಣಗಳನ್ನು ಫಿಲ್ಟರ್ ಮಾಡಿ',
        table_pipeline_title: 'ಫಲಾನುಭವಿ ಪೈಪ್‌ಲೈನ್ ಅವಲೋಕನ',
        btn_refresh: 'ತಾಜಾಗೊಳಿಸಿ',
        th_id: 'ಐಡಿ',
        th_name: 'ಹೆಸರು',
        th_phone: 'ದೂರವಾಣಿ',
        th_region: 'ಪ್ರದೇಶ / ಜಿಲ್ಲೆ',
        th_status: 'ಪ್ರಸ್ತುತ ಸ್ಥಿತಿ',
        th_date: 'ನೋಂದಣಿ ದಿನಾಂಕ',
        th_action: 'ಕ್ರಮ',
        call_agent_name: 'ಸಾಥಿ AI ಧ್ವನಿ ಅಧಿಕಾರಿ',
        fallback_title: '⌨️ ಟೈಪ್ ಮಾಡಿ ಉತ್ತರಿಸಿ',
        btn_submit: 'ಸಲ್ಲಿಸಿ',
        ctrl_replay: 'ಮತ್ತೆ ಕೇಳಿ',
        ctrl_type: 'ಟೈಪ್ ಮಾಡಿ',
        ctrl_end_call: 'ಕರೆ ಮುಗಿಸಿ',
        btn_close: 'ಮುಚ್ಚಿ',
        btn_view_roadmap: '🗺️ ಜೀವನೋಪಾಯ ಮಾರ್ಗಸೂಚಿ ವೀಕ್ಷಿಸಿ'
    }
};

// =============================================================================
// SPEECH SYNTHESIS ENGINE (ROBUST BROWSER TTS)
// =============================================================================
let _ttsWatchdog = null;

function speakText(text, lang = 'en-IN', onEndCallback) {
    if (!chatVoiceEnabled || !('speechSynthesis' in window)) {
        if (onEndCallback) onEndCallback();
        return;
    }

    try {
        if (window.speechSynthesis.paused) {
            window.speechSynthesis.resume();
        }
        window.speechSynthesis.cancel();
    } catch (e) {}

    if (!text || !text.trim()) {
        if (onEndCallback) onEndCallback();
        return;
    }

    const utterance = new SpeechSynthesisUtterance(text);
    utterance.lang = lang;
    utterance.rate = 0.95;
    utterance.pitch = 1.0;

    // Pick matching voice
    const voices = window.speechSynthesis.getVoices();
    if (voices && voices.length > 0) {
        const prefix = lang.split('-')[0];
        const match = voices.find(v => v.lang === lang || v.lang.startsWith(prefix) || (lang.startsWith('en') && v.name.includes('India')));
        if (match) utterance.voice = match;
    }

    // Retain global reference to prevent Chrome garbage-collection bug
    window._activeUtterance = utterance;

    let callbackFired = false;
    const fireCallback = () => {
        if (callbackFired) return;
        callbackFired = true;
        if (_ttsWatchdog) clearTimeout(_ttsWatchdog);
        window._activeUtterance = null;
        if (onEndCallback) onEndCallback();
    };

    utterance.onend = fireCallback;
    utterance.onerror = fireCallback;

    // Safety fallback timer in case browser drops onend
    const safetyDuration = Math.max(2200, text.length * 90);
    _ttsWatchdog = setTimeout(fireCallback, safetyDuration);

    try {
        window.speechSynthesis.speak(utterance);
    } catch (err) {
        console.warn('SpeechSynthesis error:', err);
        fireCallback();
    }
}

// Warm up voices
if ('speechSynthesis' in window) {
    window.speechSynthesis.onvoiceschanged = () => {
        window.speechSynthesis.getVoices();
    };
}

// =============================================================================
// APPLICATION INITIALIZATION & NAVIGATION
// =============================================================================
document.addEventListener('DOMContentLoaded', () => {
    initNavigation();
    loadDashboard();
    loadBeneficiaries();
    loadCatalogs();
    setAppLanguage(currentAppLang);

    const initTime = document.getElementById('chat-init-time');
    if (initTime) initTime.textContent = formatChatTime();
});

function initNavigation() {
    const navItems = document.querySelectorAll('.nav-item');
    navItems.forEach(item => {
        item.addEventListener('click', () => {
            const tabId = item.getAttribute('data-tab');
            switchTab(tabId);
        });
    });
}

function switchTab(tabId) {
    document.querySelectorAll('.nav-item').forEach(el => el.classList.remove('active'));
    document.querySelectorAll('.tab-pane').forEach(el => el.classList.remove('active'));

    const btn = document.querySelector(`.nav-item[data-tab="${tabId}"]`);
    const pane = document.getElementById(`tab-${tabId}`);

    if (btn && pane) {
        btn.classList.add('active');
        pane.classList.add('active');
    }

    if (tabId === 'dashboard') loadDashboard();
    if (tabId === 'profiler') loadBeneficiaries();
}

function changeGlobalLanguage(lang) {
    currentAppLang = lang;
    const sel = document.getElementById('global-lang-select');
    if (sel && sel.value !== lang) sel.value = lang;
    setAppLanguage(lang);
}

function setAppLanguage(lang) {
    currentAppLang = lang;
    const dict = I18N[lang] || I18N['en-IN'];

    document.querySelectorAll('[data-i18n]').forEach(el => {
        const key = el.getAttribute('data-i18n');
        if (dict[key]) el.textContent = dict[key];
    });

    const regLang = document.getElementById('reg-lang');
    if (regLang) {
        const langMap = { 'te-IN': 'Telugu', 'ta-IN': 'Tamil', 'hi-IN': 'Hindi', 'kn-IN': 'Kannada', 'en-IN': 'English' };
        if (langMap[lang]) regLang.value = langMap[lang];
    }
}

// =============================================================================
// 1. DASHBOARD LOGIC
// =============================================================================
async function loadDashboard() {
    try {
        const res = await fetch('/api/dashboard');
        const metrics = await res.json();

        document.getElementById('m-total').innerText = metrics.total;
        document.getElementById('m-enrolled').innerText = metrics.enrolled + metrics.inProgress;
        document.getElementById('m-placed').innerText = metrics.placed + metrics.selfEmployed;
        document.getElementById('m-support').innerText = metrics.needsSupport;

        const banner = document.getElementById('hitl-banner');
        if (metrics.needsSupport > 0) {
            banner.classList.remove('hidden');
        } else {
            banner.classList.add('hidden');
        }

        const bRes = await fetch('/api/beneficiaries');
        allBeneficiaries = await bRes.json();
        renderDashboardTable(allBeneficiaries);
        populateDropdowns(allBeneficiaries);

    } catch (err) {
        console.error('Error loading dashboard:', err);
    }
}

function renderDashboardTable(list) {
    const tbody = document.getElementById('dashboard-table-body');
    tbody.innerHTML = '';

    if (list.length === 0) {
        tbody.innerHTML = '<tr><td colspan="7" class="text-center">No beneficiaries found in database.</td></tr>';
        return;
    }

    list.forEach(b => {
        const tr = document.createElement('tr');
        const badgeClass = getStatusBadgeClass(b.status);

        tr.innerHTML = `
            <td><strong>${b.id}</strong></td>
            <td>${escapeHtml(b.name)}</td>
            <td>${escapeHtml(b.phone)}</td>
            <td>${escapeHtml(b.region)}</td>
            <td><span class="badge ${badgeClass}">${escapeHtml(b.status)}</span></td>
            <td>${b.registrationDate}</td>
            <td>
                <button class="btn btn-primary btn-sm" onclick="openProfileModal('${b.id}')">👁️ Profile</button>
                <button class="btn btn-secondary btn-sm" onclick="openStatusModal('${b.id}')">✏️ Status</button>
            </td>
        `;
        tbody.appendChild(tr);
    });
}

function filterDashboardLive() {
    const q = document.getElementById('dash-search').value.toLowerCase();
    const filtered = allBeneficiaries.filter(b => 
        b.name.toLowerCase().includes(q) || 
        b.id.toLowerCase().includes(q) || 
        b.region.toLowerCase().includes(q) ||
        b.phone.includes(q)
    );
    renderDashboardTable(filtered);
}

function filterDashboardTable(status) {
    const filtered = allBeneficiaries.filter(b => b.status === status);
    renderDashboardTable(filtered);
}

// =============================================================================
// 2. INTERACTIVE TURN-TAKING AI VOICE CALL AGENT (Option 1)
// =============================================================================
const CallAgent = {
    sessionId: null,
    language: 'en-IN',
    step: 1,
    state: 'IDLE',
    timerInterval: null,
    seconds: 0,
    recognition: null,
    lastPrompt: '',

    reset() {
        this.sessionId = null;
        this.step = 1;
        this.state = 'IDLE';
        this.seconds = 0;
        this.lastPrompt = '';
        if (this.timerInterval) clearInterval(this.timerInterval);
        if (this.recognition) {
            try { this.recognition.abort(); } catch(e){}
            this.recognition = null;
        }
        if (window.speechSynthesis) {
            try { window.speechSynthesis.cancel(); } catch(e){}
        }
    }
};

async function startInteractiveVoiceCall(preLang) {
    const lang = preLang || currentAppLang || 'en-IN';
    CallAgent.reset();
    CallAgent.language = lang;

    const modal = document.getElementById('voice-call-modal');
    modal.classList.add('active');
    document.body.style.overflow = 'hidden';

    const stream = document.getElementById('call-transcript-stream');
    if (stream) stream.innerHTML = '';

    const badgeMap = {
        'te-IN': '🇮🇳 Telugu (తెలుగు)',
        'ta-IN': '🇮🇳 Tamil (தமிழ்)',
        'hi-IN': '🇮🇳 Hindi (हिंदी)',
        'kn-IN': '🇮🇳 Kannada (ಕನ್ನಡ)',
        'en-IN': '🇬🇧 English'
    };
    document.getElementById('call-lang-badge').textContent = badgeMap[lang] || lang;

    document.getElementById('call-timer').textContent = '00:00';
    CallAgent.timerInterval = setInterval(() => {
        CallAgent.seconds++;
        const m = String(Math.floor(CallAgent.seconds / 60)).padStart(2, '0');
        const s = String(CallAgent.seconds % 60).padStart(2, '0');
        document.getElementById('call-timer').textContent = `${m}:${s}`;
    }, 1000);

    appendCallTranscript('ai', '📞 Connected with Saathi AI Voice Officer...');

    try {
        const res = await fetch('/api/onboarding/start', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ mode: 'call', language: lang })
        });
        const data = await res.json();
        if (data.error) {
            alert('Error: ' + data.error);
            confirmEndCall();
            return;
        }

        CallAgent.sessionId = data.sessionId;
        CallAgent.step = 1;
        executeCallAiTurn(data.prompt);

    } catch(err) {
        alert('Could not connect to Saathi server.');
        confirmEndCall();
    }
}

function executeCallAiTurn(promptText) {
    CallAgent.state = 'AI_SPEAKING';
    CallAgent.lastPrompt = promptText;

    setCallBanner('AI Speaking...', 'Please listen to the question', '🤖', 'state-speaking');
    const wave = document.getElementById('call-wave-container');
    if (wave) wave.className = 'call-wave-container wave-speaking';

    appendCallTranscript('ai', promptText);

    speakText(promptText, CallAgent.language, () => {
        if (CallAgent.state === 'AI_SPEAKING') {
            startCallUserListeningTurn();
        }
    });
}

function startCallUserListeningTurn() {
    const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SR) {
        setCallBanner('Mic Unavailable', 'Please use the "Type" button below to answer', '⌨️', 'state-retry');
        toggleCallFallback(true);
        return;
    }

    CallAgent.state = 'LISTENING';
    setCallBanner('Listening...', 'Please speak your response clearly now', '🎤', 'state-listening');
    const wave = document.getElementById('call-wave-container');
    if (wave) wave.className = 'call-wave-container wave-listening';

    const recognition = new SR();
    recognition.lang = CallAgent.language;
    recognition.interimResults = false;
    recognition.continuous = false;
    CallAgent.recognition = recognition;

    recognition.onresult = async (event) => {
        const transcript = event.results[0][0].transcript.trim();
        if (transcript) {
            appendCallTranscript('user', transcript);
            await submitCallAnswer(transcript);
        }
    };

    recognition.onerror = (e) => {
        if (CallAgent.state === 'LISTENING') {
            setCallBanner('Waiting for voice...', 'Click Replay 🔊 or Type ⌨️ if needed', '👂', 'state-speaking');
        }
    };

    recognition.onend = () => {
        if (CallAgent.state === 'LISTENING') {
            setTimeout(() => {
                if (CallAgent.state === 'LISTENING') {
                    try { recognition.start(); } catch(e){}
                }
            }, 600);
        }
    };

    try {
        recognition.start();
    } catch(e) {
        console.warn('Speech recognition error:', e);
    }
}

async function submitCallAnswer(answerText) {
    CallAgent.state = 'PROCESSING';
    setCallBanner('Processing...', 'Validating response...', '⚙️', 'state-processing');
    const wave = document.getElementById('call-wave-container');
    if (wave) wave.className = 'call-wave-container';

    try {
        const res = await fetch('/api/onboarding/transcribe-and-reply', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sessionId: CallAgent.sessionId, text: answerText })
        });
        const data = await res.json();

        if (data.error) {
            appendCallTranscript('ai', '⚠️ ' + data.error);
            executeCallAiTurn(data.error);
            return;
        }

        if (!data.valid) {
            CallAgent.state = 'RETRY';
            setCallBanner('Invalid Response', 'Please clarify your response', '⚠️', 'state-retry');
            executeCallAiTurn(data.nextPrompt);
            return;
        }

        if (data.done) {
            CallAgent.state = 'COMPLETED';
            appendCallTranscript('ai', '🎉 Registration Complete! Finalizing your profile...');
            await finalizeCallSession();
        } else {
            CallAgent.step = data.step + 1;
            executeCallAiTurn(data.nextPrompt);
        }

    } catch(err) {
        setCallBanner('Connection Error', 'Please check server connection', '❌', 'state-retry');
    }
}

async function finalizeCallSession() {
    try {
        const res = await fetch('/api/onboarding/complete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sessionId: CallAgent.sessionId })
        });
        const data = await res.json();
        if (data.success) {
            const congratsMsg = 'Your beneficiary profile has been created successfully! Assigned ID: ' + data.beneficiaryId;
            appendCallTranscript('ai', congratsMsg);
            speakText(congratsMsg, CallAgent.language);

            setTimeout(() => {
                confirmEndCall();
                loadDashboard();
                openProfileModal(data.beneficiaryId);
            }, 3200);
        }
    } catch(err) {
        alert('Error finalizing profile.');
    }
}

function setCallBanner(title, desc, icon, stateClass) {
    const banner = document.getElementById('call-state-banner');
    if (banner) banner.className = `call-state-banner ${stateClass || ''}`;
    document.getElementById('call-state-title').textContent = title;
    document.getElementById('call-state-desc').textContent = desc;
    document.getElementById('call-state-icon').textContent = icon;
}

function appendCallTranscript(role, text) {
    const stream = document.getElementById('call-transcript-stream');
    if (!stream) return;
    const div = document.createElement('div');
    div.className = `call-bubble bubble-${role}`;
    div.innerHTML = `<p>${escapeHtml(text)}</p><span class="bubble-time">${formatChatTime()}</span>`;
    stream.appendChild(div);
    stream.scrollTop = stream.scrollHeight;
}

function confirmEndCall() {
    CallAgent.reset();
    const modal = document.getElementById('voice-call-modal');
    modal.classList.remove('active');
    document.body.style.overflow = '';
}

function replayCallPrompt() {
    if (CallAgent.lastPrompt) {
        executeCallAiTurn(CallAgent.lastPrompt);
    }
}

function toggleCallFallback(force) {
    const drawer = document.getElementById('call-fallback-drawer');
    if (force !== undefined) {
        drawer.classList.toggle('active', force);
    } else {
        drawer.classList.toggle('active');
    }
    if (drawer.classList.contains('active')) {
        document.getElementById('call-fallback-input')?.focus();
    }
}

function submitCallFallback() {
    const input = document.getElementById('call-fallback-input');
    const val = input.value.trim();
    if (!val) return;
    input.value = '';
    toggleCallFallback(false);
    appendCallTranscript('user', val);
    submitCallAnswer(val);
}

// =============================================================================
// 3. PUSH-TO-TALK & ONBOARDING MODAL CONTROLLER (Option 2 & Option 3)
// =============================================================================
const OnboardingController = {
    sessionId: null,
    mode: 'direct',
    language: 'en-IN',
    currentStep: 1,
    lastPrompt: '',
    pttRecognition: null,
    pttActive: false,
    pttTranscript: '',

    reset() {
        this.sessionId = null;
        this.mode = 'direct';
        this.language = 'en-IN';
        this.currentStep = 1;
        this.lastPrompt = '';
        this.pttTranscript = '';
        if (this.pttRecognition) { try { this.pttRecognition.abort(); } catch(e){} this.pttRecognition = null; }
    }
};

function openOnboardingModal(preMode) {
    OnboardingController.reset();
    const modal = document.getElementById('onboarding-modal');
    modal.classList.add('active');
    document.body.style.overflow = 'hidden';
    if (preMode) {
        selectMode(preMode);
    } else {
        showObScreen('mode');
    }
}

function openPttModal() {
    openOnboardingModal('voice');
}

function closeOnboardingModal() {
    const modal = document.getElementById('onboarding-modal');
    modal.classList.remove('active');
    document.body.style.overflow = '';
    if (OnboardingController.pttRecognition) {
        try { OnboardingController.pttRecognition.abort(); } catch(e){}
    }
    if (window.speechSynthesis) window.speechSynthesis.cancel();
}

function restartOnboarding() {
    closeOnboardingModal();
    setTimeout(() => openOnboardingModal(), 200);
}

function showObScreen(name) {
    document.querySelectorAll('.ob-screen').forEach(s => s.classList.add('hidden'));
    const target = document.getElementById(`ob-screen-${name}`);
    if (target) target.classList.remove('hidden');
}

function selectMode(mode) {
    OnboardingController.mode = mode;
    showObScreen('lang');
    document.querySelectorAll('.ob-mode-card').forEach(c => c.classList.remove('selected'));
    const card = document.getElementById(`mode-card-${mode}`);
    if (card) card.classList.add('selected');
}

async function selectLanguage(lang) {
    OnboardingController.language = lang;
    document.querySelectorAll('.ob-lang-pill').forEach(p => p.classList.remove('selected'));
    const pill = document.getElementById('lang-' + lang.split('-')[0]);
    if (pill) pill.classList.add('selected');

    showObScreen('qa');
    setupQaMode(OnboardingController.mode);

    try {
        const res = await fetch('/api/onboarding/start', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ mode: OnboardingController.mode, language: lang })
        });
        const data = await res.json();
        if (data.error) { alert('Error: ' + data.error); return; }

        OnboardingController.sessionId = data.sessionId;
        OnboardingController.currentStep = 1;
        setObPrompt(data.prompt, lang, 1);
    } catch(err) {
        alert('Failed to connect to server.');
        closeOnboardingModal();
    }
}

function setupQaMode(mode) {
    const textArea = document.getElementById('ob-answer-text-area');
    const pttArea  = document.getElementById('ob-ptt-area');
    const ivrArea  = document.getElementById('ob-ivr-area');
    const modeLabel = document.getElementById('ob-mode-label');

    textArea.classList.add('hidden');
    pttArea.classList.add('hidden');
    ivrArea.classList.add('hidden');

    if (mode === 'direct') {
        textArea.classList.remove('hidden');
        if (modeLabel) modeLabel.textContent = '📝 Direct';
    } else if (mode === 'voice') {
        pttArea.classList.remove('hidden');
        if (modeLabel) modeLabel.textContent = '🎤 Voice';
    } else if (mode === 'ivr') {
        ivrArea.classList.remove('hidden');
        if (modeLabel) modeLabel.textContent = '📞 IVR';
        ivrClear();
    }
}

function setObPrompt(promptText, lang, step) {
    OnboardingController.lastPrompt = promptText;
    const bubble = document.getElementById('ob-prompt-text');
    if (bubble) {
        bubble.textContent = promptText;
        bubble.classList.remove('prompt-animate');
        void bubble.offsetWidth;
        bubble.classList.add('prompt-animate');
    }
    const label = document.getElementById('ob-step-label');
    const fill  = document.getElementById('ob-progress-fill');
    if (label) label.textContent = `Step ${step} of 10`;
    if (fill) fill.style.width = `${Math.min(step * 10, 100)}%`;

    const errEl = document.getElementById('ob-error');
    if (errEl) errEl.textContent = '';

    speakText(promptText, lang || OnboardingController.language);

    const input = document.getElementById('ob-answer-input');
    if (input) { input.value = ''; input.focus(); }
    const pttT = document.getElementById('ob-ptt-transcript');
    if (pttT) pttT.textContent = '';
    OnboardingController.pttTranscript = '';
    const pttNext = document.getElementById('ob-ptt-next-btn');
    if (pttNext) pttNext.disabled = true;
    ivrClear();
}

async function submitObStep() {
    const mode = OnboardingController.mode;
    let answer = '';

    if (mode === 'direct') {
        answer = (document.getElementById('ob-answer-input')?.value || '').trim();
    } else if (mode === 'voice') {
        answer = OnboardingController.pttTranscript.trim();
    } else if (mode === 'ivr') {
        answer = (document.getElementById('ob-ivr-input')?.textContent || '').trim();
    }

    if (!answer) {
        const errEl = document.getElementById('ob-error');
        if (errEl) errEl.textContent = '⚠️ Please provide an answer before continuing.';
        return;
    }

    ['ob-next-btn','ob-ptt-next-btn'].forEach(id => {
        const el = document.getElementById(id);
        if (el) { el.disabled = true; el.textContent = '⏳...'; }
    });

    try {
        const res = await fetch('/api/onboarding/transcribe-and-reply', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sessionId: OnboardingController.sessionId, text: answer })
        });
        const data = await res.json();

        if (data.error) {
            const errEl = document.getElementById('ob-error');
            if (errEl) errEl.textContent = '❌ ' + data.error;
            return;
        }

        if (!data.valid) {
            const errEl = document.getElementById('ob-error');
            if (errEl) errEl.textContent = '⚠️ Invalid answer. Please try again.';
            setObPrompt(data.nextPrompt, OnboardingController.language, data.step);
            return;
        }

        if (data.done) {
            setObPrompt(data.nextPrompt, OnboardingController.language, 10);
            setTimeout(() => completeOnboardingSession(), 1800);
        } else {
            OnboardingController.currentStep = data.step + 1;
            setObPrompt(data.nextPrompt, OnboardingController.language, data.step + 1);
        }
    } catch(err) {
        const errEl = document.getElementById('ob-error');
        if (errEl) errEl.textContent = '❌ Connection error.';
    } finally {
        ['ob-next-btn','ob-ptt-next-btn'].forEach(id => {
            const el = document.getElementById(id);
            if (el) { el.disabled = false; el.textContent = el.id === 'ob-ptt-next-btn' ? 'Submit →' : 'Next →'; }
        });
    }
}

async function completeOnboardingSession() {
    try {
        const res = await fetch('/api/onboarding/complete', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ sessionId: OnboardingController.sessionId })
        });
        const data = await res.json();
        if (data.success) {
            document.getElementById('ob-success-id').textContent = data.beneficiaryId;
            document.getElementById('ob-success-status').textContent = data.status;
            showObScreen('success');
            speakText('Registration complete! Your profile has been saved.', OnboardingController.language);
        }
    } catch(err) {
        alert('Connection error during completion.');
    }
}

function replayTts() {
    speakText(OnboardingController.lastPrompt, OnboardingController.language);
}

function startObVoiceInput() {
    const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SR) return;

    const recognition = new SR();
    recognition.lang = OnboardingController.language;
    recognition.interimResults = false;
    const micBtn = document.getElementById('ob-mic-btn');
    if (micBtn) { micBtn.textContent = '🔴'; micBtn.disabled = true; }

    recognition.onresult = (e) => {
        const transcript = e.results[0][0].transcript;
        const input = document.getElementById('ob-answer-input');
        if (input) input.value = transcript;
    };
    recognition.onend = () => { if (micBtn) { micBtn.textContent = '🎤'; micBtn.disabled = false; } };
    recognition.onerror = () => { if (micBtn) { micBtn.textContent = '🎤'; micBtn.disabled = false; } };
    recognition.start();
}

function startPtt() {
    const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SR || OnboardingController.pttActive) return;

    OnboardingController.pttActive = true;
    OnboardingController.pttTranscript = '';
    const ring = document.getElementById('ob-ptt-ring');
    const label = document.getElementById('ob-ptt-label');
    const tEl = document.getElementById('ob-ptt-transcript');
    if (ring) ring.classList.add('ptt-active');
    if (label) label.textContent = '🔴 Listening...';
    if (tEl) tEl.textContent = '';

    if (window.speechSynthesis) window.speechSynthesis.cancel();

    const recognition = new SR();
    recognition.lang = OnboardingController.language;
    recognition.interimResults = true;
    recognition.continuous = false;
    OnboardingController.pttRecognition = recognition;

    recognition.onresult = (e) => {
        let interim = '', final = '';
        for (let i = e.resultIndex; i < e.results.length; i++) {
            if (e.results[i].isFinal) final += e.results[i][0].transcript;
            else interim += e.results[i][0].transcript;
        }
        if (tEl) tEl.textContent = final || interim;
        if (final) {
            OnboardingController.pttTranscript = final;
            const pttNext = document.getElementById('ob-ptt-next-btn');
            if (pttNext) pttNext.disabled = false;
        }
    };
    recognition.onerror = () => stopPtt();
    recognition.onend   = () => stopPtt();
    recognition.start();
}

function stopPtt() {
    OnboardingController.pttActive = false;
    const ring = document.getElementById('ob-ptt-ring');
    const label = document.getElementById('ob-ptt-label');
    if (ring) ring.classList.remove('ptt-active');
    if (label) label.textContent = 'Hold to Speak';
    if (OnboardingController.pttRecognition) {
        try { OnboardingController.pttRecognition.stop(); } catch(e){}
    }
}

function ivrKey(char) {
    const el = document.getElementById('ob-ivr-input');
    if (el) el.textContent += char;
}
function ivrBackspace() {
    const el = document.getElementById('ob-ivr-input');
    if (el) el.textContent = el.textContent.slice(0, -1);
}
function ivrClear() {
    const el = document.getElementById('ob-ivr-input');
    if (el) el.textContent = '';
}

// =============================================================================
// 4. MULTILINGUAL PROFILE MODAL (Telugu, Tamil, Hindi, Kannada, English)
// =============================================================================
async function openProfileModal(benId, lang) {
    currentModalBenId = benId;
    const activeLang = lang || currentAppLang || 'en-IN';

    const modal = document.getElementById('profile-modal');
    modal.classList.add('active');

    const switchSelect = document.getElementById('prof-lang-switch');
    if (switchSelect) switchSelect.value = activeLang;

    const body = document.getElementById('profile-modal-body');
    body.innerHTML = '<div class="empty-state">Loading localized profile...</div>';

    try {
        const res = await fetch(`/api/beneficiary?id=${encodeURIComponent(benId)}&lang=${encodeURIComponent(activeLang)}`);
        const b = await res.json();

        if (b.error) {
            body.innerHTML = `<div class="empty-state">❌ ${b.error}</div>`;
            return;
        }

        document.getElementById('prof-modal-name').textContent = b.name;
        document.getElementById('prof-modal-id').textContent = b.id;

        const L = b.labels;

        let recsHtml = '';
        if (b.recommendedPrograms && b.recommendedPrograms.length > 0) {
            recsHtml = b.recommendedPrograms.map(r => `
                <div class="prof-rec-card">
                    <div class="prof-rec-header">
                        <span>🎓 ${escapeHtml(r.programName)}</span>
                        <span class="badge badge-success">Match: ${r.score} pts</span>
                    </div>
                    <div class="prof-rec-meta">
                        <span><strong>NSQF Level:</strong> ${r.nsqfLevel}</span>
                        <span><strong>Duration:</strong> ${r.duration}</span>
                        <span><strong>Region:</strong> ${escapeHtml(r.region)}</span>
                    </div>
                </div>
            `).join('');
        } else {
            recsHtml = '<div class="empty-state">No specific training program generated yet.</div>';
        }

        body.innerHTML = `
            <div class="prof-meta-grid">
                <div class="prof-detail-box">
                    <div class="p-label">${escapeHtml(L.name)}</div>
                    <div class="p-value">${escapeHtml(b.name)}</div>
                </div>
                <div class="prof-detail-box">
                    <div class="p-label">${escapeHtml(L.phone)}</div>
                    <div class="p-value">${escapeHtml(b.phone)}</div>
                </div>
                <div class="prof-detail-box">
                    <div class="p-label">${escapeHtml(L.education)}</div>
                    <div class="p-value">${escapeHtml(b.education)}</div>
                </div>
                <div class="prof-detail-box">
                    <div class="p-label">${escapeHtml(L.currentLivelihood)}</div>
                    <div class="p-value">${escapeHtml(b.currentLivelihood)}</div>
                </div>
                <div class="prof-detail-box">
                    <div class="p-label">${escapeHtml(L.familyOccupation)}</div>
                    <div class="p-value">${escapeHtml(b.familyOccupation)}</div>
                </div>
                <div class="prof-detail-box">
                    <div class="p-label">${escapeHtml(L.employmentPreference)}</div>
                    <div class="p-value">${escapeHtml(b.employmentPreference)}</div>
                </div>
                <div class="prof-detail-box">
                    <div class="p-label">${escapeHtml(L.skills)}</div>
                    <div class="p-value">${escapeHtml(b.skills)}</div>
                </div>
                <div class="prof-detail-box">
                    <div class="p-label">${escapeHtml(L.interests)}</div>
                    <div class="p-value">${escapeHtml(b.interests)}</div>
                </div>
                <div class="prof-detail-box">
                    <div class="p-label">${escapeHtml(L.aspirations)}</div>
                    <div class="p-value">${escapeHtml(b.aspirations)}</div>
                </div>
                <div class="prof-detail-box">
                    <div class="p-label">${escapeHtml(L.constraints)}</div>
                    <div class="p-value">${escapeHtml(b.constraints)}</div>
                </div>
                <div class="prof-detail-box">
                    <div class="p-label">${escapeHtml(L.region)}</div>
                    <div class="p-value">${escapeHtml(b.region)}</div>
                </div>
                <div class="prof-detail-box">
                    <div class="p-label">${escapeHtml(L.status)}</div>
                    <div class="p-value"><span class="badge ${getStatusBadgeClass(b.status)}">${escapeHtml(b.status)}</span></div>
                </div>
            </div>

            <div class="prof-section-title">
                <span>🎯</span>
                <span>${escapeHtml(L.recommendedTraining)}</span>
            </div>
            <div class="prof-recs-grid">
                ${recsHtml}
            </div>
        `;

    } catch(err) {
        body.innerHTML = '<div class="empty-state">Failed to load beneficiary profile.</div>';
    }
}

function switchProfileModalLanguage(lang) {
    if (currentModalBenId) {
        openProfileModal(currentModalBenId, lang);
    }
}

function closeProfileModal() {
    const modal = document.getElementById('profile-modal');
    modal.classList.remove('active');
    currentModalBenId = null;
}

function generateRoadmapForModalBen() {
    if (currentModalBenId) {
        closeProfileModal();
        switchTab('roadmap');
        const sel = document.getElementById('roadmap-ben-select');
        if (sel) {
            sel.value = currentModalBenId;
            loadPersonalRoadmap();
        }
    }
}

// =============================================================================
// 5. REGISTRATION FORM, PROFILES & CATALOGS
// =============================================================================
async function handleRegistration(e) {
    e.preventDefault();

    const payload = {
        name: document.getElementById('reg-name').value.trim(),
        phone: document.getElementById('reg-phone').value.trim(),
        language: document.getElementById('reg-lang').value,
        education: document.getElementById('reg-edu').value,
        familyOccupation: document.getElementById('reg-famocc').value.trim(),
        currentLivelihood: document.getElementById('reg-curliv').value.trim(),
        skills: document.getElementById('reg-skills').value.trim(),
        interests: document.getElementById('reg-interests').value.trim(),
        aspirations: document.getElementById('reg-aspirations').value.trim(),
        constraints: document.getElementById('reg-constraints').value.trim(),
        employmentPreference: document.getElementById('reg-emppref').value,
        region: document.getElementById('reg-region').value.trim()
    };

    try {
        const res = await fetch('/api/register', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify(payload)
        });
        const data = await res.json();

        if (data.success) {
            alert(`✓ Beneficiary Registered Successfully!\nAssigned ID: ${data.id}\nSaved to SQLite saathi.db`);
            document.getElementById('reg-form').reset();
            loadDashboard();
            loadBeneficiaries();
        } else {
            alert('Error: ' + data.error);
        }
    } catch (err) {
        alert('Failed to connect to server.');
    }
}

async function loadBeneficiaries() {
    try {
        const res = await fetch('/api/beneficiaries');
        allBeneficiaries = await res.json();
        renderProfileList(allBeneficiaries);
        populateDropdowns(allBeneficiaries);
    } catch (err) {
        console.error(err);
    }
}

function renderProfileList(list) {
    const container = document.getElementById('profile-list');
    container.innerHTML = '';

    if (list.length === 0) {
        container.innerHTML = '<div class="empty-state">No beneficiaries registered yet.</div>';
        return;
    }

    list.forEach(b => {
        const item = document.createElement('div');
        item.className = 'profile-list-item';
        item.style.padding = '12px';
        item.style.borderBottom = '1px solid #e2e8f0';
        item.style.cursor = 'pointer';
        item.onclick = () => showProfileDetail(b);
        item.innerHTML = `
            <div class="p-name" style="font-weight:700; color:var(--text-main);">${escapeHtml(b.name)} <small style="color:var(--text-muted);">(${b.id})</small></div>
            <div class="p-meta" style="font-size:12px; color:var(--text-muted); margin-top:2px;">📍 ${escapeHtml(b.region)} | 📞 ${escapeHtml(b.phone)}</div>
            <div class="badge ${getStatusBadgeClass(b.status)} mt-5">${escapeHtml(b.status)}</div>
        `;
        container.appendChild(item);
    });
}

function showProfileDetail(b) {
    const detail = document.getElementById('profile-detail');
    detail.innerHTML = `
        <div class="profile-card-header" style="display:flex; justify-content:space-between; align-items:center; margin-bottom:12px;">
            <h4>${escapeHtml(b.name)} <span class="badge badge-indigo">${b.id}</span></h4>
            <span class="badge ${getStatusBadgeClass(b.status)}">${escapeHtml(b.status)}</span>
        </div>
        <div class="profile-card-body" style="font-size:13px; display:flex; flex-direction:column; gap:6px;">
            <div><strong>Language:</strong> ${escapeHtml(b.language)}</div>
            <div><strong>Phone:</strong> ${escapeHtml(b.phone)}</div>
            <div><strong>Education:</strong> ${escapeHtml(b.education)}</div>
            <div><strong>Current Livelihood:</strong> ${escapeHtml(b.currentLivelihood)}</div>
            <div><strong>Family Occupation:</strong> ${escapeHtml(b.familyOccupation)}</div>
            <div><strong>Skills:</strong> ${escapeHtml(b.skills)}</div>
            <div><strong>Interests:</strong> ${escapeHtml(b.interests)}</div>
            <div><strong>Aspirations:</strong> ${escapeHtml(b.aspirations)}</div>
            <div><strong>Constraints:</strong> ${escapeHtml(b.constraints)}</div>
            <div><strong>Employment Pref:</strong> ${escapeHtml(b.employmentPreference)}</div>
            <div><strong>Region:</strong> ${escapeHtml(b.region)}</div>
            <div><strong>Registered:</strong> ${b.registrationDate}</div>
        </div>
        <div style="margin-top:14px; display:flex; gap:8px;">
            <button class="btn btn-primary btn-sm" onclick="openProfileModal('${b.id}')">👁️ Full Multilingual Profile</button>
            <button class="btn btn-secondary btn-sm" onclick="openStatusModal('${b.id}')">✏️ Update Pipeline Status</button>
        </div>
    `;
}

function filterProfilesLive() {
    const q = document.getElementById('profile-search').value.toLowerCase();
    const filtered = allBeneficiaries.filter(b => 
        b.name.toLowerCase().includes(q) || 
        b.id.toLowerCase().includes(q) || 
        b.region.toLowerCase().includes(q)
    );
    renderProfileList(filtered);
}

// =============================================================================
// 6. RECOMMENDATIONS, ROADMAP & CATALOGS
// =============================================================================
function populateDropdowns(list) {
    const selects = [
        document.getElementById('training-ben-select'),
        document.getElementById('job-ben-select'),
        document.getElementById('roadmap-ben-select'),
        document.getElementById('chat-ben-select')
    ];

    selects.forEach(sel => {
        if (!sel) return;
        const currentVal = sel.value;
        sel.innerHTML = sel.id === 'chat-ben-select' ? '<option value="">No Beneficiary Context</option>' : '';

        list.forEach(b => {
            const opt = document.createElement('option');
            opt.value = b.id;
            opt.textContent = `${b.name} (${b.id}) - ${b.region}`;
            sel.appendChild(opt);
        });

        if (currentVal) sel.value = currentVal;
    });
}

async function runTrainingRecommendations() {
    const benId = document.getElementById('training-ben-select').value;
    if (!benId) return alert('Please select a beneficiary.');

    const container = document.getElementById('training-results');
    container.innerHTML = '<div class="empty-state">Running AI Recommendation Algorithm...</div>';

    try {
        const res = await fetch(`/api/recommendations/training?id=${benId}`);
        const data = await res.json();

        if (data.length === 0) {
            container.innerHTML = '<div class="empty-state">No matching training programs found.</div>';
            return;
        }

        container.innerHTML = data.map((rec, i) => `
            <div class="rec-card ${i === 0 ? 'top-match' : ''}">
                <div class="rec-header" style="display:flex; justify-content:space-between; align-items:center;">
                    <h4>${escapeHtml(rec.name || rec.programName)}</h4>
                    <span class="match-score badge badge-success">★ Match Score: ${rec.score}</span>
                </div>
                <div class="rec-meta" style="font-size:12px; color:var(--text-muted); margin:6px 0;">
                    <span><strong>NSQF Level:</strong> ${rec.nsqfLevel}</span> |
                    <span><strong>Duration:</strong> ${rec.duration}</span> |
                    <span><strong>Region:</strong> ${escapeHtml(rec.region)}</span> |
                    <span><strong>Employment:</strong> ${escapeHtml(rec.employmentType)}</span>
                </div>
                <div class="rec-desc" style="font-size:13px; margin-bottom:8px;">${escapeHtml(rec.description)}</div>
                <div class="rec-reasons" style="font-size:12px;">
                    <strong>Why this program was matched:</strong>
                    <ul>${rec.reasons.map(r => `<li>✓ ${escapeHtml(r)}</li>`).join('')}</ul>
                </div>
            </div>
        `).join('');

        loadDashboard();
    } catch (err) {
        container.innerHTML = '<div class="empty-state">Error generating recommendations.</div>';
    }
}

async function runJobRecommendations() {
    const benId = document.getElementById('job-ben-select').value;
    if (!benId) return alert('Please select a beneficiary.');

    const container = document.getElementById('job-results');
    container.innerHTML = '<div class="empty-state">Matching with Local Opportunities...</div>';

    try {
        const res = await fetch(`/api/recommendations/jobs?id=${benId}`);
        const data = await res.json();

        if (data.length === 0) {
            container.innerHTML = '<div class="empty-state">No matching job opportunities found.</div>';
            return;
        }

        container.innerHTML = data.map((rec, i) => `
            <div class="rec-card ${i === 0 ? 'top-match' : ''}">
                <div class="rec-header" style="display:flex; justify-content:space-between; align-items:center;">
                    <h4>${escapeHtml(rec.name || rec.opportunityName)}</h4>
                    <span class="match-score badge badge-success">★ Score: ${rec.score}</span>
                </div>
                <div class="rec-meta" style="font-size:12px; color:var(--text-muted); margin:6px 0;">
                    <span><strong>Type:</strong> ${escapeHtml(rec.type)}</span> |
                    <span><strong>Required Skill:</strong> ${escapeHtml(rec.requiredSkill)}</span> |
                    <span><strong>Region:</strong> ${escapeHtml(rec.region)}</span>
                </div>
                <div class="rec-desc" style="font-size:13px; margin-bottom:8px;">${escapeHtml(rec.description)}</div>
                <div class="rec-reasons" style="font-size:12px;">
                    <strong>Match Criteria:</strong>
                    <ul>${rec.reasons.map(r => `<li>✓ ${escapeHtml(r)}</li>`).join('')}</ul>
                </div>
            </div>
        `).join('');

        loadDashboard();
    } catch (err) {
        container.innerHTML = '<div class="empty-state">Error generating job matches.</div>';
    }
}

async function loadPersonalRoadmap() {
    const benId = document.getElementById('roadmap-ben-select').value;
    if (!benId) return alert('Please select a beneficiary.');

    const container = document.getElementById('roadmap-results');
    container.innerHTML = '<div class="empty-state">Generating Personalized Livelihood Roadmap...</div>';

    try {
        const res = await fetch(`/api/roadmap?id=${benId}`);
        const data = await res.json();

        if (!data.skillGaps) {
            container.innerHTML = '<div class="empty-state">Could not generate roadmap for this beneficiary.</div>';
            return;
        }

        const b = data.beneficiary;
        container.innerHTML = `
            <div class="glass-card mb-20" style="background:#eff6ff; border:1px solid #bfdbfe; padding:16px; border-radius:12px; margin-bottom:16px;">
                <h3 style="font-size:16px; font-weight:800; color:#1e40af;">Roadmap for ${escapeHtml(b.name)} (${b.id})</h3>
                <p style="margin-top:4px; font-size:13px; color:#1e40af;">
                    Target Program: <strong>${escapeHtml(data.targetProgram)} (NSQF Level ${data.nsqfLevel})</strong> | 
                    Target Role: <strong>${escapeHtml(data.targetRole)}</strong>
                </p>
                <div style="margin-top:8px; font-size:12px; color:#1e3a8a;">
                    <strong>Identified Skill Gaps to Bridge:</strong> ${data.skillGaps.length > 0 ? data.skillGaps.join(', ') : 'None (Fully Qualified!)'}
                </div>
            </div>
            
            <div class="roadmap-step" style="background:#ffffff; border:1px solid #e2e8f0; border-left:4px solid var(--primary); border-radius:12px; padding:14px; margin-bottom:10px;">
                <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:6px;">
                    <h4 style="color:var(--primary); font-size:14px; font-weight:700;">Step 1: Current Baseline & Skill Gap Analysis</h4>
                    <span class="badge badge-success">✓ Completed</span>
                </div>
                <p style="font-size:13px; color:var(--text-main); margin-bottom:6px;">Beneficiary profiled with skills (${escapeHtml(b.skills)}). Identified ${data.skillGaps.length} critical skills to develop.</p>
            </div>
            <div class="step-arrow" style="text-align:center; color:#94a3b8; font-size:18px; margin:4px 0;">↓</div>

            <div class="roadmap-step" style="background:#ffffff; border:1px solid #e2e8f0; border-left:4px solid var(--secondary); border-radius:12px; padding:14px; margin-bottom:10px;">
                <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:6px;">
                    <h4 style="color:var(--secondary); font-size:14px; font-weight:700;">Step 2: Enrollment in ${escapeHtml(data.targetProgram)}</h4>
                    <span class="badge badge-warning">⏳ Next Action</span>
                </div>
                <p style="font-size:13px; color:var(--text-main); margin-bottom:6px;">Duration: ${escapeHtml(data.duration)}. Covers: ${escapeHtml(data.requiredSkills)}.</p>
            </div>
            <div class="step-arrow" style="text-align:center; color:#94a3b8; font-size:18px; margin:4px 0;">↓</div>

            <div class="roadmap-step" style="background:#ffffff; border:1px solid #e2e8f0; border-left:4px solid #10b981; border-radius:12px; padding:14px; margin-bottom:10px;">
                <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:6px;">
                    <h4 style="color:#10b981; font-size:14px; font-weight:700;">Step 3: Placement into ${escapeHtml(data.targetRole)} (${escapeHtml(data.targetType)})</h4>
                    <span class="badge badge-secondary">Upcoming</span>
                </div>
                <p style="font-size:13px; color:var(--text-main); margin-bottom:6px;">Post-certification linkage with local employers and enterprise credit schemes.</p>
            </div>
        `;
    } catch (err) {
        container.innerHTML = '<div class="empty-state">Error loading roadmap.</div>';
    }
}

async function loadCatalogs() {
    try {
        const res = await fetch('/api/catalogs');
        catalogData = await res.json();
        showCatalog('tp');
    } catch (err) {
        console.error(err);
    }
}

function showCatalog(type) {
    document.querySelectorAll('.subtab-btn').forEach(b => b.classList.remove('active'));
    const btn = document.querySelector(`.subtab-btn[onclick*="${type}"]`);
    if (btn) btn.classList.add('active');

    const thead = document.getElementById('catalog-thead');
    const tbody = document.getElementById('catalog-tbody');

    if (type === 'tp') {
        thead.innerHTML = `
            <tr>
                <th>ID</th>
                <th>Program Name</th>
                <th>NSQF Level</th>
                <th>Region</th>
                <th>Employment Type</th>
                <th>Duration</th>
                <th>Keywords</th>
            </tr>
        `;
        tbody.innerHTML = catalogData.trainingPrograms.map(p => `
            <tr>
                <td><strong>${p.id || p.trainingId}</strong></td>
                <td>${escapeHtml(p.name || p.programName)}</td>
                <td><span class="badge badge-info">Level ${p.nsqfLevel}</span></td>
                <td>${escapeHtml(p.region)}</td>
                <td>${escapeHtml(p.type || p.employmentType)}</td>
                <td>${escapeHtml(p.duration)}</td>
                <td><small>${escapeHtml(p.skills || p.skillsKeywords)}</small></td>
            </tr>
        `).join('');
    } else {
        thead.innerHTML = `
            <tr>
                <th>ID</th>
                <th>Opportunity Name</th>
                <th>Type</th>
                <th>Required Skill</th>
                <th>Region</th>
                <th>Description</th>
            </tr>
        `;
        tbody.innerHTML = catalogData.opportunities.map(o => `
            <tr>
                <td><strong>${o.id || o.opportunityId}</strong></td>
                <td>${escapeHtml(o.name || o.opportunityName)}</td>
                <td><span class="badge ${o.type.includes('Self') ? 'badge-primary' : 'badge-success'}">${escapeHtml(o.type)}</span></td>
                <td>${escapeHtml(o.requiredSkill)}</td>
                <td>${escapeHtml(o.region)}</td>
                <td>${escapeHtml(o.description)}</td>
            </tr>
        `).join('');
    }
}

// =============================================================================
// 7. AI VOICE HUB & CHAT ASSISTANT
// =============================================================================
function formatChatTime() {
    return new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
}

function toggleChatVoice() {
    chatVoiceEnabled = !chatVoiceEnabled;
    const btn = document.getElementById('chat-voice-toggle');
    btn.textContent = chatVoiceEnabled ? '🔊 Voice On' : '🔇 Voice Off';
    btn.style.opacity = chatVoiceEnabled ? '1' : '0.5';
}

async function sendChatMessage() {
    const input = document.getElementById('chat-input');
    const msg = input.value.trim();
    if (!msg) return;

    appendChatBubble('user', msg);
    input.value = '';

    const benId = document.getElementById('chat-ben-select')?.value || '';
    const sendBtn = document.getElementById('chat-send-btn');
    sendBtn.disabled = true;
    sendBtn.textContent = '...';

    try {
        const res = await fetch('/api/ai-chat', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ message: msg, beneficiaryId: benId })
        });
        const data = await res.json();
        if (data.success) {
            appendChatBubble('ai', data.reply);
            speakText(data.reply, currentAppLang);
        } else {
            appendChatBubble('ai', '⚠️ ' + (data.error || 'Could not process that. Please try again.'));
        }
    } catch (err) {
        appendChatBubble('ai', '⚠️ Connection error with Saathi server.');
    } finally {
        sendBtn.disabled = false;
        sendBtn.textContent = 'Send';
    }
}

function appendChatBubble(role, text) {
    const win = document.getElementById('chat-window');
    if (!win) return;
    const div = document.createElement('div');
    div.className = `chat-msg chat-msg-${role}`;
    const formatted = text.replace(/\n/g, '<br>');
    div.innerHTML = role === 'ai'
        ? `<div class="chat-avatar">🤖</div><div class="chat-bubble"><p>${formatted}</p><span class="chat-time">${formatChatTime()}</span></div>`
        : `<div class="chat-bubble user-bubble"><p>${escapeHtml(text)}</p><span class="chat-time">${formatChatTime()}</span></div><div class="chat-avatar user-avatar">👤</div>`;
    win.appendChild(div);
    win.scrollTop = win.scrollHeight;
}

function startVoiceMessage() {
    const SR = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!SR) return alert('Speech Recognition is not supported in this browser. Please use Chrome or Edge.');

    const recognition = new SR();
    recognition.lang = currentAppLang || 'en-IN';
    recognition.interimResults = false;
    recognition.maxAlternatives = 1;

    const status = document.getElementById('voice-msg-status');
    const micBtn = document.getElementById('chat-mic-btn');
    if (status) status.textContent = '🎙 Listening...';
    if (micBtn) micBtn.classList.add('mic-active');

    recognition.onresult = async (event) => {
        const transcript = event.results[0][0].transcript;
        const chatInput = document.getElementById('chat-input');
        if (chatInput) chatInput.value = transcript;
        await sendChatMessage();
        if (status) status.textContent = '';
    };

    recognition.onerror = () => {
        if (status) status.textContent = '❌ Speech recognition error.';
        if (micBtn) micBtn.classList.remove('mic-active');
    };

    recognition.onend = () => {
        if (micBtn) micBtn.classList.remove('mic-active');
    };

    recognition.start();
}

function simulateVoiceInput() {
    document.getElementById('reg-name').value = 'Ramesh Kumar';
    document.getElementById('reg-phone').value = '9876543210';
    document.getElementById('reg-lang').value = 'Telugu';
    document.getElementById('reg-edu').value = '10th Pass';
    document.getElementById('reg-famocc').value = 'Handloom Weaving';
    document.getElementById('reg-curliv').value = 'Daily Wage Worker';
    document.getElementById('reg-skills').value = 'Basic Sewing, Handloom Operation';
    document.getElementById('reg-interests').value = 'Garment Making, Tailoring';
    document.getElementById('reg-aspirations').value = 'Start Local Tailoring Shop';
    document.getElementById('reg-constraints').value = 'Local District Salem / Tirupati';
    document.getElementById('reg-emppref').value = 'Self Employment';
    document.getElementById('reg-region').value = 'Rural Salem';
    alert('🎤 Voice Simulation: Auto-populated sample beneficiary data into form!');
}

// =============================================================================
// 8. STATUS UPDATE MODAL & UTILITIES
// =============================================================================
function openStatusModal(benId) {
    const ben = allBeneficiaries.find(b => b.id === benId);
    if (!ben) return;

    document.getElementById('modal-ben-id').value = ben.id;
    document.getElementById('modal-ben-name').textContent = `Update Status for ${ben.name} (${ben.id})`;
    document.getElementById('modal-status-select').value = ben.status;
    document.getElementById('status-modal').classList.add('active');
}

function closeStatusModal() {
    document.getElementById('status-modal').classList.remove('active');
}

async function submitStatusUpdate() {
    const benId = document.getElementById('modal-ben-id').value;
    const newStatus = document.getElementById('modal-status-select').value;

    try {
        const res = await fetch('/api/update-status', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ beneficiaryId: benId, status: newStatus })
        });
        const data = await res.json();

        if (data.success) {
            closeStatusModal();
            loadDashboard();
            loadBeneficiaries();
        } else {
            alert('Error updating status: ' + data.error);
        }
    } catch (err) {
        alert('Failed to connect to server.');
    }
}

function getStatusBadgeClass(status) {
    switch (status) {
        case 'Profile Created': return 'badge-secondary';
        case 'Recommendation Generated': return 'badge-primary';
        case 'Enrolled':
        case 'Training In Progress': return 'badge-warning';
        case 'Training Completed': return 'badge-info';
        case 'Placed':
        case 'Self-Employment Started': return 'badge-success';
        case 'Needs Officer Support': return 'badge-danger';
        default: return 'badge-secondary';
    }
}

function escapeHtml(text) {
    if (!text) return '';
    return String(text)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
}
