var wakeActive = false;
function toggleWakeWord() {
    wakeActive = !wakeActive;
    var btn = document.getElementById("wake-btn");
    var st = document.getElementById("status");
    if (btn) btn.textContent = wakeActive ? "🎙️ ON" : "🎙️ OFF";
    if (st) st.textContent = wakeActive ? "SAY 'HEY CHINU'..." : "TAP ORB TO SPEAK";
    if (window.Android && Android.setWakeWordMode) {
        Android.setWakeWordMode(wakeActive);
    }
}
document.addEventListener("DOMContentLoaded", () => {
    const orb = document.querySelector("#orb, .orb, .orb-container");
    const micBtn = document.querySelector("#mic-btn, #micBtn, .mic-btn");
    const sendBtn = document.querySelector("#send-btn, #sendBtn, .send-btn");
    const flashBtn = document.querySelector("#flash-btn, #flashBtn, .flash-btn");
    const input = document.querySelector("#user-input, #userInput, input[type='text']");
    const chatBox = document.querySelector("#chat-box, #chatBox, .chat-box, #output");
    const statusText = document.querySelector("#status, .status-text, #orb-status");

    function updateClock() {
        const now = new Date();
        const timeEl = document.querySelector("#time, #clock, .time");
        const dateEl = document.querySelector("#date, .date");
        if (timeEl) timeEl.textContent = now.toTimeString().split(" ")[0];
        if (dateEl) dateEl.textContent = now.toDateString().toUpperCase();
    }
    setInterval(updateClock, 1000);
    updateClock();

    if (navigator.getBattery) {
        navigator.getBattery().then(b => {
            const batEl = document.querySelector("#battery, #battery-level, .battery");
            const updateBat = () => { if (batEl) batEl.textContent = Math.round(b.level * 100) + "%"; };
            updateBat();
            b.addEventListener("levelchange", updateBat);
        });
    }

    function showReply(text, lang = "gu") {
        if (chatBox) {
            chatBox.innerHTML = `<div class="msg chinu"><b>CHINU:</b> ${text}</div>`;
        }
        if (window.Android && Android.speak) {
            Android.speak(text, lang);
        }
    }

    function isHindi(cmd) {
        const hindiWords = ["kaise", "kaisi", "kya", "batao", "kholo", "karo", "tum", "tumhara", "mera", "samay", "kitne", "baje", "hindi", "shukriya", "thak gaya", "udas", "bhejo", "chalao"];
        return hindiWords.some(w => cmd.includes(w));
    }

    window.onListeningStart = () => {
        if (statusText) statusText.textContent = "LISTENING...";
        if (orb) orb.classList.add("listening");
    };

    window.onListeningEnd = () => {
        if (statusText) statusText.textContent = "TAP ORB TO SPEAK";
        if (orb) orb.classList.remove("listening");
    };

    window.onVoiceResult = (spokenText) => {
        handleCommand(spokenText);
    };

    function startVoice() {
        if (window.Android && Android.startListening) {
            Android.startListening();
        }
    }

    function handleCommand(rawText) {
        const cmd = rawText.trim().toLowerCase();
        if (!cmd) return;
        const hi = isHindi(cmd);

        // 1. YouTube Search (e.g., "youtube par arijit singh search kar" or "play song on youtube")
        if (cmd.includes("youtube") && (cmd.includes("search") || cmd.includes("play") || cmd.includes("chalav") || cmd.includes("chalao") || cmd.includes("par ") || cmd.includes("ma "))) {
            let q = cmd.replace(/youtube|par|ma|on|search|kar|karo|play|chalav|chalao/g, "").trim();
            if (q && window.Android && Android.searchYouTube) {
                if (hi) showReply(`यूट्यूब पर ${q} सर्च कर रही हूँ।`, "hi");
                else showReply(`યુટ્યુબ પર ${q} સર્ચ કરી રહી છું.`, "gu");
                Android.searchYouTube(q);
                return;
            }
        }

        // 2. WhatsApp Message (e.g., "whatsapp message hello kem cho")
        if ((cmd.includes("whatsapp") && (cmd.includes("message") || cmd.includes("msg") || cmd.includes("mokal") || cmd.includes("bhejo"))) || cmd.startsWith("message ") || cmd.startsWith("msg ")) {
            let msg = rawText.replace(/whatsapp|message|msg|par|ma|mokal|bhejo|send|kar|karo/gi, "").trim();
            if (msg && window.Android && Android.sendWhatsApp) {
                if (hi) showReply("व्हाट्सएप खोल रही हूँ, कृपया कॉन्टैक्ट चुनें।", "hi");
                else showReply("વોટ્સએપ ખોલી રહી છું, કોન્ટેક્ટ સિલેક્ટ કરીને સેન્ડ કરી દો.", "gu");
                Android.sendWhatsApp(msg);
                return;
            }
        }

        // 3. Call specific number (e.g., "call 9876543210")
        const phoneMatch = cmd.match(/\d{7,12}/);
        if ((cmd.includes("call") || cmd.includes("dial") || cmd.includes("phone")) && phoneMatch) {
            if (window.Android && Android.dialNumber) {
                if (hi) showReply(`${phoneMatch[0]} पर कॉल लगा रही हूँ।`, "hi");
                else showReply(`${phoneMatch[0]} પર કોલ લગાવી રહી છું.`, "gu");
                Android.dialNumber(phoneMatch[0]);
                return;
            }
        }

        // 4. Flashlight
        if (cmd.includes("torch") || cmd.includes("flash") || cmd.includes("light")) {
            if (window.Android && Android.toggleFlashlight) {
                const isOn = Android.toggleFlashlight();
                if (hi) showReply(isOn ? "फ्लैशलाइट चालू कर दी है।" : "फ्लैशलाइट बंद कर दी है।", "hi");
                else showReply(isOn ? "ફ્લેશલાઇટ ચાલુ કરી દીધી છે." : "ફ્લેશલાઇટ બંધ કરી દીધી છે.", "gu");
            }
            return;
        }

        // 5. Open Apps
        const apps = ["whatsapp", "youtube", "instagram", "insta", "camera", "settings", "setting", "chrome", "phone", "call", "dialer"];
        for (const app of apps) {
            if (cmd.includes(app) && (cmd.includes("open") || cmd.includes("khol") || cmd.includes("chalu") || cmd.includes("start") || cmd === app)) {
                if (window.Android && Android.openApp) {
                    const ok = Android.openApp(app);
                    if (hi) showReply(ok ? `${app} खोल रही हूँ।` : `आपके फोन में ${app} नहीं मिला।`, "hi");
                    else showReply(ok ? `${app} ખોલી રહી છું.` : `તમારા ફોનમાં ${app} મળ્યું નથી.`, "gu");
                }
                return;
            }
        }

        // 6. Time & Date
        if (cmd.includes("time") || cmd.includes("samay") || cmd.includes("vagya") || cmd.includes("baje")) {
            const t = new Date().toLocaleTimeString("en-IN", { hour: "2-digit", minute: "2-digit" });
            if (hi) showReply(`अभी ${t} बजे हैं।`, "hi");
            else showReply(`અત્યારે ${t} વાગ્યા છે.`, "gu");
            return;
        }
        if (cmd.includes("date") || cmd.includes("tarikh") || cmd.includes("din")) {
            const d = new Date().toLocaleDateString("en-IN", { weekday: "long", day: "numeric", month: "long" });
            if (hi) showReply(`आज ${d} है।`, "hi");
            else showReply(`આજે ${d} છે.`, "gu");
            return;
        }

        // 7. Google Search
        if (cmd.startsWith("search ") || cmd.includes("google par search") || cmd.includes("google search")) {
            const q = cmd.replace(/google par search|google search|search/g, "").trim();
            if (q && window.Android && Android.searchGoogle) {
                if (hi) showReply(`गूगल पर ${q} सर्च कर रही हूँ।`, "hi");
                else showReply(`ગૂગલ પર ${q} સર્ચ કરી રહી છું.`, "gu");
                Android.searchGoogle(q);
            }
            return;
        }

        // 8. Chat & Emotions
        if (cmd.includes("hello") || cmd.includes("hi") || cmd.includes("hey") || cmd.includes("namaste") || cmd.includes("kem cho") || cmd.includes("kem chho") || cmd.includes("kaise ho") || cmd.includes("kaisi ho")) {
            if (hi) showReply("नमस्ते! मैं बिल्कुल ठीक हूँ। बताइए, आज आपके लिए क्या करूँ?", "hi");
            else showReply("હેલો! હું એકદમ મજામાં છું. બોલો, આજે તમારા માટે શું કામ કરું?", "gu");
            return;
        }
        if (cmd.includes("good morning")) {
            if (hi) showReply("गुड मॉर्निंग! आपका दिन बहुत अच्छा जाए।", "hi");
            else showReply("ગુડ મોર્નિંગ! તમારો આજનો દિવસ ખૂબ સરસ જાય.", "gu");
            return;
        }
        if (cmd.includes("good night")) {
            if (hi) showReply("गुड नाइट! आराम कीजिए, कल फिर मिलेंगे।", "hi");
            else showReply("ગુડ નાઇટ! આરામ કરો, કાલે પાછા મળીશું.", "gu");
            return;
        }
        if (cmd.includes("naam") || cmd.includes("name") || cmd.includes("who are you") || cmd.includes("kon")) {
            if (hi) showReply("मेरा नाम चीनू है। मैं आपकी पर्सनल एआई असिस्टेंट हूँ।", "hi");
            else showReply("મારું નામ ચીનુ છે. હું તમારી પર્સનલ એઆઈ આસિસ્ટન્ટ છું.", "gu");
            return;
        }
        if (cmd.includes("sad") || cmd.includes("mood kharab") || cmd.includes("tension") || cmd.includes("thak") || cmd.includes("udas")) {
            if (hi) showReply("चिंता मत कीजिए, मैं आपके साथ हूँ। थोड़ा आराम कर लीजिए, सब ठीक हो जाएगा।", "hi");
            else showReply("ચિંતા ના કરો, હું તમારી સાથે જ છું. થોડો આરામ કરી લો, બધું બરાબર થઈ જશે.", "gu");
            return;
        }
        if (cmd.includes("thank") || cmd.includes("shukriya") || cmd.includes("aabhar")) {
            if (hi) showReply("आपका स्वागत है! आपकी मदद करके मुझे हमेशा खुशी होती है।", "hi");
            else showReply("વેલકમ! તમારી મદદ કરીને મને હંમેશા ખુશી થાય છે.", "gu");
            return;
        }

        // 9. Default Offline Reply
        if (hi) {
            showReply("मैंने आपकी बात सुनी। आप यूट्यूब सर्च, व्हाट्सएप मैसेज, कॉल या फ्लैशलाइट के लिए कह सकते हैं।", "hi");
        } else {
            showReply("મેં તમારી વાત સાંભળી. તમે યુટ્યુબ સર્ચ, વોટ્સએપ મેસેજ, કોલ કે ફ્લેશલાઇટ માટે કહી શકો છો.", "gu");
        }
    }

    if (orb) orb.addEventListener("click", startVoice);
    if (micBtn) micBtn.addEventListener("click", startVoice);
    if (flashBtn) flashBtn.addEventListener("click", () => handleCommand("flashlight"));
    if (sendBtn && input) {
        sendBtn.addEventListener("click", () => {
            handleCommand(input.value);
            input.value = "";
        });
        input.addEventListener("keypress", (e) => {
            if (e.key === "Enter") {
                handleCommand(input.value);
                input.value = "";
            }
        });
    }
});
        // 0. Hey Chinu + Siri Glow Animation
        if (cmd.includes("hey chinu") || cmd === "chinu" || cmd.includes("hi chinu")) {
            if (orb) {
                orb.classList.add("siri-glow");
                setTimeout(() => orb.classList.remove("siri-glow"), 4000);
            }
            if (hi) showReply("जी बोलिए, मैं सुन रही हूँ। क्या काम है?", "hi");
            else showReply("હા બોલો, હું સાંભળી રહી છું. શું કામ છે?", "gu");
            return;
        }
