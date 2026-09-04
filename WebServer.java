import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import java.awt.Desktop;
import java.io.*;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * ============================================================================
 * Saathi – Embedded REST API & Web Server
 * Runs a zero-dependency local web server on port 8080 connected to saathi.db
 * ============================================================================
 */
public class WebServer {

    private static final int PORT = 8080;

    public static void main(String[] args) {
        try {
            // Ensure Database initialized
            Main.initDatabase();
            Main.loadSampleData();
            Main.loadData();

            HttpServer server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // API Handlers
            server.createContext("/api/beneficiaries", new BeneficiariesHandler());
            server.createContext("/api/beneficiary",   new BeneficiaryDetailHandler());
            server.createContext("/api/register", new RegisterHandler());
            server.createContext("/api/update-status", new UpdateStatusHandler());
            server.createContext("/api/recommendations/training", new TrainingRecHandler());
            server.createContext("/api/recommendations/jobs", new JobRecHandler());
            server.createContext("/api/roadmap", new RoadmapHandler());
            server.createContext("/api/dashboard", new DashboardHandler());
            server.createContext("/api/catalogs", new CatalogsHandler());
            server.createContext("/api/ai-chat", new AiChatHandler());
            server.createContext("/api/recordings", new RecordingHandler());

            // Onboarding (Conversational Intake) Handlers
            server.createContext("/api/onboarding/start",                new OnboardingStartHandler());
            server.createContext("/api/onboarding/transcribe-and-reply", new OnboardingReplyHandler());
            server.createContext("/api/onboarding/complete",             new OnboardingCompleteHandler());

            // ── New Required Endpoints ─────────────────────────────────────────────
            // POST /api/onboarding/session  — unified start-or-advance
            server.createContext("/api/onboarding/session",              new OnboardingSessionHandler());
            // POST /api/beneficiary/direct  — AI-voice direct registration
            server.createContext("/api/beneficiary/direct",              new BeneficiaryDirectHandler());
            // GET  /api/beneficiary/{id}/roadmap — path-param roadmap
            server.createContext("/api/beneficiary/",                    new BeneficiaryRouterHandler());
            // GET  /api/officer/metrics     — officer dashboard with pipeline array
            server.createContext("/api/officer/metrics",                 new OfficerMetricsHandler());

            // Ensure recordings directory exists
            new File("recordings").mkdirs();

            // Static File Handler
            server.createContext("/", new StaticFileHandler());

            server.setExecutor(null);
            server.start();

            String serverUrl = "http://localhost:" + PORT;
            System.out.println("=================================================");
            System.out.println("🚀 Saathi Web Application Server Started!");
            System.out.println("🌐 URL: " + serverUrl);
            System.out.println("💾 Database: saathi.db Connected");
            System.out.println("=================================================");

            // Auto open browser
            try {
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(serverUrl));
                }
            } catch (java.net.URISyntaxException | IOException ignored) {}

        } catch (IOException e) {
            System.err.println("[X] Failed to start Web Server: " + e.getMessage());
        }
    }

    // Static File Handler (Serves web/ index.html, styles.css, app.js)
    static class StaticFileHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String path = exchange.getRequestURI().getPath();
            if (path.equals("/") || path.isEmpty()) {
                path = "/index.html";
            }

            File file = new File("web" + path);
            if (!file.exists() || file.isDirectory()) {
                sendResponse(exchange, 404, "text/plain", "404 Not Found");
                return;
            }

            String contentType = "text/html";
            if (path.endsWith(".css")) contentType = "text/css";
            else if (path.endsWith(".js")) contentType = "text/javascript";
            else if (path.endsWith(".json")) contentType = "application/json";
            else if (path.endsWith(".png")) contentType = "image/png";
            else if (path.endsWith(".webm")) contentType = "audio/webm";
            else if (path.endsWith(".mp3")) contentType = "audio/mpeg";
            else if (path.endsWith(".wav")) contentType = "audio/wav";

            byte[] bytes = readFileToBytes(file);
            exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
            exchange.sendResponseHeaders(200, bytes.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(bytes);
            }
        }

        private byte[] readFileToBytes(File file) throws IOException {
            try (FileInputStream fis = new FileInputStream(file);
                 ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[4096];
                int len;
                while ((len = fis.read(buffer)) != -1) {
                    baos.write(buffer, 0, len);
                }
                return baos.toByteArray();
            }
        }
    }

    // GET /api/beneficiaries
    static class BeneficiariesHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                StringBuilder json = new StringBuilder("[");
                List<Main.Beneficiary> list = Main.getBeneficiaries();
                for (int i = 0; i < list.size(); i++) {
                    json.append(beneficiaryToJson(list.get(i)));
                    if (i < list.size() - 1) json.append(",");
                }
                json.append("]");
                sendResponse(exchange, 200, "application/json", json.toString());
            } else {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            }
        }
    }

    // GET /api/beneficiary?id=...&lang=...
    static class BeneficiaryDetailHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { exchange.sendResponseHeaders(204, -1); return; }
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}"); return;
            }

            Map<String, String> query = queryToMap(exchange.getRequestURI().getQuery());
            String id = query.getOrDefault("id", "").trim();
            String lang = query.getOrDefault("lang", "en-IN").trim();

            Main.Beneficiary b = Main.findBeneficiaryById(id);
            if (b == null) {
                sendResponse(exchange, 404, "application/json", "{\"error\":\"Beneficiary not found\"}");
                return;
            }

            Map<String, String> labels = Main.LocalizedProfileDictionary.getLabels(lang);

            // Compute recommendations
            List<Main.Recommendation<Main.TrainingProgram>> recs = Main.calculateTrainingRecommendations(b);
            StringBuilder recsJson = new StringBuilder("[");
            for (int i = 0; i < recs.size(); i++) {
                Main.Recommendation<Main.TrainingProgram> r = recs.get(i);
                recsJson.append(String.format(
                    "{\"programName\":\"%s\",\"nsqfLevel\":%d,\"region\":\"%s\",\"duration\":\"%s\",\"score\":%d}",
                    escapeJson(r.getItem().getProgramName()),
                    r.getItem().getNsqfLevel(),
                    escapeJson(r.getItem().getRegion()),
                    escapeJson(r.getItem().getDuration()),
                    r.getScore()
                ));
                if (i < recs.size() - 1) recsJson.append(",");
            }
            recsJson.append("]");

            // Build full localized JSON
            String json = String.format(
                "{\"id\":\"%s\",\"name\":\"%s\",\"phone\":\"%s\",\"language\":\"%s\",\"education\":\"%s\","
                + "\"familyOccupation\":\"%s\",\"currentLivelihood\":\"%s\",\"skills\":\"%s\",\"interests\":\"%s\","
                + "\"aspirations\":\"%s\",\"constraints\":\"%s\",\"employmentPreference\":\"%s\",\"region\":\"%s\","
                + "\"status\":\"%s\",\"registrationDate\":\"%s\","
                + "\"labels\":{\"name\":\"%s\",\"phone\":\"%s\",\"education\":\"%s\",\"familyOccupation\":\"%s\","
                + "\"currentLivelihood\":\"%s\",\"skills\":\"%s\",\"interests\":\"%s\",\"aspirations\":\"%s\","
                + "\"constraints\":\"%s\",\"employmentPreference\":\"%s\",\"region\":\"%s\",\"status\":\"%s\","
                + "\"recommendedTraining\":\"%s\",\"recommendedJobs\":\"%s\"},"
                + "\"recommendedPrograms\":%s}",
                escapeJson(b.getId()), escapeJson(b.getName()), escapeJson(b.getPhone()),
                escapeJson(b.getLanguage()), escapeJson(b.getEducation()), escapeJson(b.getFamilyOccupation()),
                escapeJson(b.getCurrentLivelihood()), escapeJson(b.getSkills()), escapeJson(b.getInterests()),
                escapeJson(b.getAspirations()), escapeJson(b.getConstraints()), escapeJson(b.getEmploymentPreference()),
                escapeJson(b.getRegion()), escapeJson(b.getStatus()), escapeJson(b.getRegistrationDate()),
                escapeJson(labels.get("name")), escapeJson(labels.get("phone")), escapeJson(labels.get("education")),
                escapeJson(labels.get("familyOccupation")), escapeJson(labels.get("currentLivelihood")),
                escapeJson(labels.get("skills")), escapeJson(labels.get("interests")), escapeJson(labels.get("aspirations")),
                escapeJson(labels.get("constraints")), escapeJson(labels.get("employmentPreference")),
                escapeJson(labels.get("region")), escapeJson(labels.get("status")),
                escapeJson(labels.get("recommendedTraining")), escapeJson(labels.get("recommendedJobs")),
                recsJson.toString()
            );

            sendResponse(exchange, 200, "application/json", json);
        }
    }

    // POST /api/register
    static class RegisterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                Map<String, String> params = parseJsonOrForm(body);

                String name = params.getOrDefault("name", "").trim();
                String phone = params.getOrDefault("phone", "").trim();
                String lang = params.getOrDefault("language", "English");
                String edu = params.getOrDefault("education", "12th Pass");
                String famOcc = params.getOrDefault("familyOccupation", "None");
                String curLiv = params.getOrDefault("currentLivelihood", "Unemployed");
                String skills = params.getOrDefault("skills", "Basic Communication");
                String interests = params.getOrDefault("interests", "General Employment");
                String aspirations = params.getOrDefault("aspirations", "Gainful Livelihood");
                String constraints = params.getOrDefault("constraints", "None");
                String empPref = params.getOrDefault("employmentPreference", "Both");
                String region = params.getOrDefault("region", "District Level");

                if (name.isEmpty() || phone.isEmpty()) {
                    sendResponse(exchange, 400, "application/json", "{\"error\":\"Name and Phone are required\"}");
                    return;
                }

                String newId = Main.registerBeneficiaryDirect(name, phone, lang, edu, famOcc, curLiv, skills, interests, aspirations, constraints, empPref, region);
                sendResponse(exchange, 200, "application/json", "{\"success\":true,\"id\":\"" + newId + "\"}");
            } else {
                sendResponse(exchange, 455, "application/json", "{\"error\":\"Method not allowed\"}");
            }
        }
    }

    // POST /api/update-status
    static class UpdateStatusHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                String body = readRequestBody(exchange);
                Map<String, String> params = parseJsonOrForm(body);

                String id = params.get("id");
                String status = params.get("status");

                Main.Beneficiary b = Main.findBeneficiaryById(id);
                if (b == null || status == null) {
                    sendResponse(exchange, 404, "application/json", "{\"error\":\"Beneficiary or status not found\"}");
                    return;
                }

                b.setStatus(status);
                Main.saveBeneficiaryToDb(b);
                sendResponse(exchange, 200, "application/json", "{\"success\":true,\"id\":\"" + id + "\",\"status\":\"" + escapeJson(status) + "\"}");
            } else {
                sendResponse(exchange, 455, "application/json", "{\"error\":\"Method not allowed\"}");
            }
        }
    }

    // GET /api/recommendations/training?id=...
    static class TrainingRecHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
            String id = params.get("id");

            Main.Beneficiary b = Main.findBeneficiaryById(id);
            if (b == null) {
                sendResponse(exchange, 404, "application/json", "{\"error\":\"Beneficiary not found\"}");
                return;
            }

            List<Main.Recommendation<Main.TrainingProgram>> recs = Main.calculateTrainingRecommendations(b);

            if (b.getStatus().equalsIgnoreCase("Profile Created")) {
                b.setStatus("Recommendation Generated");
                Main.saveBeneficiaryToDb(b);
            }

            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < recs.size(); i++) {
                Main.Recommendation<Main.TrainingProgram> r = recs.get(i);
                Main.TrainingProgram tp = r.getItem();

                json.append("{")
                    .append("\"score\":").append(r.getScore()).append(",")
                    .append("\"id\":\"").append(escapeJson(tp.getTrainingId())).append("\",")
                    .append("\"name\":\"").append(escapeJson(tp.getProgramName())).append("\",")
                    .append("\"nsqfLevel\":").append(tp.getNsqfLevel()).append(",")
                    .append("\"region\":\"").append(escapeJson(tp.getRegion())).append("\",")
                    .append("\"employmentType\":\"").append(escapeJson(tp.getEmploymentType())).append("\",")
                    .append("\"duration\":\"").append(escapeJson(tp.getDuration())).append("\",")
                    .append("\"description\":\"").append(escapeJson(tp.getDescription())).append("\",")
                    .append("\"reasons\":[");

                List<String> reasons = r.getReasons();
                for (int j = 0; j < reasons.size(); j++) {
                    json.append("\"").append(escapeJson(reasons.get(j))).append("\"");
                    if (j < reasons.size() - 1) json.append(",");
                }
                json.append("]}");
                if (i < recs.size() - 1) json.append(",");
            }
            json.append("]");

            sendResponse(exchange, 200, "application/json", json.toString());
        }
    }

    // GET /api/recommendations/jobs?id=...
    static class JobRecHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
            String id = params.get("id");

            Main.Beneficiary b = Main.findBeneficiaryById(id);
            if (b == null) {
                sendResponse(exchange, 404, "application/json", "{\"error\":\"Beneficiary not found\"}");
                return;
            }

            List<Main.Recommendation<Main.Opportunity>> recs = Main.calculateOpportunityRecommendations(b);

            StringBuilder json = new StringBuilder("[");
            for (int i = 0; i < recs.size(); i++) {
                Main.Recommendation<Main.Opportunity> r = recs.get(i);
                Main.Opportunity opp = r.getItem();

                json.append("{")
                    .append("\"score\":").append(r.getScore()).append(",")
                    .append("\"id\":\"").append(escapeJson(opp.getOpportunityId())).append("\",")
                    .append("\"name\":\"").append(escapeJson(opp.getOpportunityName())).append("\",")
                    .append("\"region\":\"").append(escapeJson(opp.getRegion())).append("\",")
                    .append("\"type\":\"").append(escapeJson(opp.getType())).append("\",")
                    .append("\"requiredSkill\":\"").append(escapeJson(opp.getRequiredSkill())).append("\",")
                    .append("\"description\":\"").append(escapeJson(opp.getDescription())).append("\",")
                    .append("\"reasons\":[");

                List<String> reasons = r.getReasons();
                for (int j = 0; j < reasons.size(); j++) {
                    json.append("\"").append(escapeJson(reasons.get(j))).append("\"");
                    if (j < reasons.size() - 1) json.append(",");
                }
                json.append("]}");
                if (i < recs.size() - 1) json.append(",");
            }
            json.append("]");

            sendResponse(exchange, 200, "application/json", json.toString());
        }
    }

    // GET /api/roadmap?id=...
    static class RoadmapHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            Map<String, String> params = queryToMap(exchange.getRequestURI().getQuery());
            String id = params.get("id");

            Main.Beneficiary b = Main.findBeneficiaryById(id);
            if (b == null) {
                sendResponse(exchange, 404, "application/json", "{\"error\":\"Beneficiary not found\"}");
                return;
            }

            List<Main.Recommendation<Main.TrainingProgram>> topTrainings = Main.calculateTrainingRecommendations(b);
            List<Main.Recommendation<Main.Opportunity>> topOpportunities = Main.calculateOpportunityRecommendations(b);

            Main.TrainingProgram topTP = topTrainings.isEmpty() ? Main.getTrainingPrograms().get(0) : topTrainings.get(0).getItem();
            Main.Opportunity topOpp = topOpportunities.isEmpty() ? Main.getOpportunities().get(0) : topOpportunities.get(0).getItem();

            List<String> missingSkills = Main.performSkillGapAnalysis(b.getSkills(), topTP.getSkillsKeywords());

            StringBuilder json = new StringBuilder("{");
            json.append("\"beneficiary\":").append(beneficiaryToJson(b)).append(",")
                .append("\"targetProgram\":\"").append(escapeJson(topTP.getProgramName())).append("\",")
                .append("\"nsqfLevel\":").append(topTP.getNsqfLevel()).append(",")
                .append("\"duration\":\"").append(escapeJson(topTP.getDuration())).append("\",")
                .append("\"requiredSkills\":\"").append(escapeJson(topTP.getSkillsKeywords())).append("\",")
                .append("\"skillGaps\":[");

            for (int i = 0; i < missingSkills.size(); i++) {
                json.append("\"").append(escapeJson(missingSkills.get(i))).append("\"");
                if (i < missingSkills.size() - 1) json.append(",");
            }
            json.append("],")
                .append("\"targetRole\":\"").append(escapeJson(topOpp.getOpportunityName())).append("\",")
                .append("\"targetType\":\"").append(escapeJson(topOpp.getType())).append("\"")
                .append("}");

            sendResponse(exchange, 200, "application/json", json.toString());
        }
    }

    // GET /api/dashboard
    static class DashboardHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            int total = Main.getBeneficiaries().size();
            int profilesCreated = 0, recGenerated = 0, enrolled = 0, inProgress = 0, completed = 0, placed = 0, selfEmployed = 0, needsSupport = 0;

            for (Main.Beneficiary b : Main.getBeneficiaries()) {
                String s = b.getStatus().toLowerCase();
                if (s.contains("profile created")) profilesCreated++;
                else if (s.contains("recommendation")) recGenerated++;
                else if (s.contains("enrolled")) enrolled++;
                else if (s.contains("in progress")) inProgress++;
                else if (s.contains("completed")) completed++;
                else if (s.contains("placed")) placed++;
                else if (s.contains("self-employment")) selfEmployed++;
                else if (s.contains("needs officer support")) needsSupport++;
            }

            String json = String.format("""
                {
                    "total": %d,
                    "profilesCreated": %d,
                    "recGenerated": %d,
                    "enrolled": %d,
                    "inProgress": %d,
                    "completed": %d,
                    "placed": %d,
                    "selfEmployed": %d,
                    "needsSupport": %d
                }
                """, total, profilesCreated, recGenerated, enrolled, inProgress, completed, placed, selfEmployed, needsSupport);

            sendResponse(exchange, 200, "application/json", json);
        }
    }

    // GET /api/catalogs
    static class CatalogsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            StringBuilder json = new StringBuilder("{\"trainingPrograms\":[");
            List<Main.TrainingProgram> tps = Main.getTrainingPrograms();
            for (int i = 0; i < tps.size(); i++) {
                Main.TrainingProgram tp = tps.get(i);
                json.append(String.format("{\"id\":\"%s\",\"name\":\"%s\",\"nsqfLevel\":%d,\"region\":\"%s\",\"type\":\"%s\",\"duration\":\"%s\",\"skills\":\"%s\",\"description\":\"%s\"}",
                        escapeJson(tp.getTrainingId()), escapeJson(tp.getProgramName()), tp.getNsqfLevel(), escapeJson(tp.getRegion()),
                        escapeJson(tp.getEmploymentType()), escapeJson(tp.getDuration()), escapeJson(tp.getSkillsKeywords()), escapeJson(tp.getDescription())));
                if (i < tps.size() - 1) json.append(",");
            }

            json.append("],\"opportunities\":[");
            List<Main.Opportunity> opps = Main.getOpportunities();
            for (int i = 0; i < opps.size(); i++) {
                Main.Opportunity opp = opps.get(i);
                json.append(String.format("{\"id\":\"%s\",\"name\":\"%s\",\"region\":\"%s\",\"type\":\"%s\",\"requiredSkill\":\"%s\",\"description\":\"%s\"}",
                        escapeJson(opp.getOpportunityId()), escapeJson(opp.getOpportunityName()), escapeJson(opp.getRegion()),
                        escapeJson(opp.getType()), escapeJson(opp.getRequiredSkill()), escapeJson(opp.getDescription())));
                if (i < opps.size() - 1) json.append(",");
            }
            json.append("]}");

            sendResponse(exchange, 200, "application/json", json.toString());
        }
    }

    // Helpers
    private static void sendResponse(HttpExchange exchange, int statusCode, String contentType, String response) throws IOException {
        byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", contentType + "; charset=utf-8");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.sendResponseHeaders(statusCode, bytes.length);
        try (OutputStream os = exchange.getResponseBody()) {
            os.write(bytes);
        }
    }

    private static String readRequestBody(HttpExchange exchange) throws IOException {
        InputStream is = exchange.getRequestBody();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = is.read(buffer)) != -1) {
            baos.write(buffer, 0, len);
        }
        return baos.toString(StandardCharsets.UTF_8);
    }

    private static Map<String, String> parseJsonOrForm(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.trim().isEmpty()) return map;

        if (body.trim().startsWith("{")) {
            // Simple JSON parser
            String trimmed = body.trim().substring(1, body.trim().length() - 1);
            String[] pairs = trimmed.split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            for (String pair : pairs) {
                String[] kv = pair.split(":(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)", 2);
                if (kv.length == 2) {
                    String k = kv[0].trim().replaceAll("^\"|\"$", "");
                    String v = kv[1].trim().replaceAll("^\"|\"$", "");
                    map.put(k, v);
                }
            }
        } else {
            // URL encoded
            String[] pairs = body.split("&");
            for (String pair : pairs) {
                String[] kv = pair.split("=");
                if (kv.length == 2) {
                    map.put(URLDecoder.decode(kv[0], StandardCharsets.UTF_8), URLDecoder.decode(kv[1], StandardCharsets.UTF_8));
                }
            }
        }
        return map;
    }

    private static Map<String, String> queryToMap(String query) {
        Map<String, String> result = new HashMap<>();
        if (query == null) return result;
        for (String param : query.split("&")) {
            String[] entry = param.split("=");
            if (entry.length > 1) {
                result.put(entry[0], URLDecoder.decode(entry[1], StandardCharsets.UTF_8));
            }
        }
        return result;
    }

    private static String beneficiaryToJson(Main.Beneficiary b) {
        return String.format(
            "{\"id\":\"%s\",\"name\":\"%s\",\"phone\":\"%s\",\"language\":\"%s\",\"education\":\"%s\"," +
            "\"familyOccupation\":\"%s\",\"currentLivelihood\":\"%s\",\"skills\":\"%s\",\"interests\":\"%s\"," +
            "\"aspirations\":\"%s\",\"constraints\":\"%s\",\"employmentPreference\":\"%s\",\"region\":\"%s\"," +
            "\"status\":\"%s\",\"registrationDate\":\"%s\"}",
            escapeJson(b.getId()), escapeJson(b.getName()), escapeJson(b.getPhone()), escapeJson(b.getLanguage()),
            escapeJson(b.getEducation()), escapeJson(b.getFamilyOccupation()), escapeJson(b.getCurrentLivelihood()),
            escapeJson(b.getSkills()), escapeJson(b.getInterests()), escapeJson(b.getAspirations()),
            escapeJson(b.getConstraints()), escapeJson(b.getEmploymentPreference()), escapeJson(b.getRegion()),
            escapeJson(b.getStatus()), escapeJson(b.getRegistrationDate())
        );
    }

    private static String escapeJson(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", " ").replace("\r", " ");
    }

    // ============================================================================
    // ONBOARDING START HANDLER — POST /api/onboarding/start
    // Body: {"mode":"voice|ivr|direct", "language":"hi-IN"}
    // ============================================================================
    static class OnboardingStartHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { exchange.sendResponseHeaders(204, -1); return; }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}"); return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> p = parseJsonOrForm(body);

            String mode = p.getOrDefault("mode", "direct").trim();
            String language = p.getOrDefault("language", "en-IN").trim();

            // Create a new session
            String sessionId = "SES" + System.currentTimeMillis() + (int)(Math.random() * 1000);
            Main.BeneficiaryOnboardingSession session =
                new Main.BeneficiaryOnboardingSession(sessionId, mode, language);

            Main.onboardingSessions.put(sessionId, session);
            Main.saveOnboardingSessionToDb(session);

            // Build first prompt (Step 1: name)
            String welcome  = Main.LocalizedPrompts.getWelcome(language);
            String step1Prompt = Main.LocalizedPrompts.getPrompt(language, 1);
            String ttsHint = Main.TextToSpeechService.synthesize(welcome + " " + step1Prompt, language);

            String json = String.format(
                "{\"sessionId\":\"%s\",\"mode\":\"%s\",\"language\":\"%s\",\"step\":1,"
                + "\"prompt\":\"%s\",\"tts\":%s}",
                escapeJson(sessionId), escapeJson(mode), escapeJson(language),
                escapeJson(welcome + " " + step1Prompt), ttsHint
            );
            sendResponse(exchange, 200, "application/json", json);
        }
    }

    // ============================================================================
    // ONBOARDING REPLY HANDLER — POST /api/onboarding/transcribe-and-reply
    // Body: {"sessionId":"...", "text":"<user answer>"}
    // ============================================================================
    static class OnboardingReplyHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { exchange.sendResponseHeaders(204, -1); return; }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}"); return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> p = parseJsonOrForm(body);

            String sessionId = p.getOrDefault("sessionId", "").trim();
            String rawText   = p.getOrDefault("text", "").trim();

            Main.BeneficiaryOnboardingSession session = Main.onboardingSessions.get(sessionId);
            if (session == null) {
                sendResponse(exchange, 404, "application/json",
                    "{\"error\":\"Session not found. Please start a new onboarding session.\"}");
                return;
            }

            if (session.isComplete()) {
                sendResponse(exchange, 200, "application/json",
                    String.format("{\"sessionId\":\"%s\",\"done\":true,\"step\":11}", escapeJson(sessionId)));
                return;
            }

            int currentStep = session.currentStep;
            String fieldKey  = session.currentFieldKey();

            // Extract & validate
            String extractedValue = Main.ConversationalExtractionService.extract(currentStep, rawText);

            if (extractedValue == null) {
                // Validation failed — ask again with an error hint
                String lang = session.language;
                String retryPrefix = switch (lang) {
                    case "hi-IN" -> "क्षमा करें, वह सही नहीं लगा। कृपया फिर से प्रयास करें। ";
                    case "ta-IN" -> "மன்னிக்கவும், மீண்டும் முயற்சிக்கவும். ";
                    case "te-IN" -> "క్షమించండి, దయచేసి మళ్ళీ ప్రయత్నించండి. ";
                    case "kn-IN" -> "ಕ್ಷಮಿಸಿ, ದಯವಿಟ್ಟು ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ. ";
                    default      -> "Sorry, that doesn't look right. Please try again. ";
                };
                String retryPrompt = retryPrefix + Main.LocalizedPrompts.getPrompt(lang, currentStep);
                String tts = Main.TextToSpeechService.synthesize(retryPrompt, lang);
                sendResponse(exchange, 200, "application/json", String.format(
                    "{\"sessionId\":\"%s\",\"step\":%d,\"field\":\"%s\",\"done\":false,"
                    + "\"valid\":false,\"nextPrompt\":\"%s\",\"tts\":%s}",
                    escapeJson(sessionId), currentStep, escapeJson(fieldKey),
                    escapeJson(retryPrompt), tts
                ));
                return;
            }

            // Advance session
            session.submitAndAdvance(extractedValue);
            Main.saveOnboardingSessionToDb(session);

            boolean done = session.isComplete();
            String lang = session.language;

            String nextPrompt = done
                ? ("en-IN".equals(lang)
                    ? "Excellent! You have completed all 10 questions. Your profile is being saved now."
                    : "hi-IN".equals(lang)
                    ? "बहुत अच्छा! सभी 10 सवाल पूरे हो गए। आपकी प्रोफ़ाइल सहेजी जा रही है।"
                    : "ta-IN".equals(lang)
                    ? "மிகவும் நன்று! 10 கேள்விகளும் முடிந்தன. உங்கள் சுயவிவரம் சேமிக்கப்படுகிறது."
                    : "te-IN".equals(lang)
                    ? "చాలా బాగుంది! 10 ప్రశ్నలు పూర్తయ్యాయి. మీ ప్రొఫైల్ సేవ్ అవుతోంది."
                    : "ಅತ್ಯುತ್ತಮ! 10 ಪ್ರಶ್ನೆಗಳು ಮುಗಿದಿವೆ. ನಿಮ್ಮ ಪ್ರೊಫೈಲ್ ಉಳಿಸಲಾಗುತ್ತಿದೆ.")
                : Main.LocalizedPrompts.getPrompt(lang, session.currentStep);

            String tts = Main.TextToSpeechService.synthesize(nextPrompt, lang);

            sendResponse(exchange, 200, "application/json", String.format(
                "{\"sessionId\":\"%s\",\"step\":%d,\"field\":\"%s\",\"value\":\"%s\","
                + "\"done\":%b,\"valid\":true,\"nextPrompt\":\"%s\",\"tts\":%s}",
                escapeJson(sessionId), currentStep, escapeJson(fieldKey),
                escapeJson(extractedValue), done, escapeJson(nextPrompt), tts
            ));
        }
    }

    // ============================================================================
    // ONBOARDING COMPLETE HANDLER — POST /api/onboarding/complete
    // Body: {"sessionId":"..."}
    // ============================================================================
    static class OnboardingCompleteHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { exchange.sendResponseHeaders(204, -1); return; }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}"); return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> p = parseJsonOrForm(body);
            String sessionId = p.getOrDefault("sessionId", "").trim();

            Main.BeneficiaryOnboardingSession session = Main.onboardingSessions.get(sessionId);
            if (session == null) {
                sendResponse(exchange, 404, "application/json",
                    "{\"error\":\"Session not found\"}"); return;
            }

            Map<String, String> d = session.collectedData;

            // Map BCP-47 lang code to the friendly label used in beneficiaries table
            String langLabel = switch (session.language) {
                case "hi-IN" -> "Hindi";
                case "ta-IN" -> "Tamil";
                case "te-IN" -> "Telugu";
                case "kn-IN" -> "Kannada";
                default      -> "English";
            };

            // Register beneficiary
            String newId = Main.registerBeneficiaryDirect(
                d.getOrDefault("name",                 "Unknown"),
                d.getOrDefault("phone",                ""),
                langLabel,
                d.getOrDefault("education",            "Not Specified"),
                d.getOrDefault("familyOccupation",     "None"),
                d.getOrDefault("currentLivelihood",    "Unemployed"),
                d.getOrDefault("skills",               "Basic Communication"),
                d.getOrDefault("interests",            "General Employment"),
                d.getOrDefault("aspirations",          "Gainful Livelihood"),
                d.getOrDefault("constraints",          "None"),
                d.getOrDefault("employmentPreference", "Both"),
                d.getOrDefault("region",               "District Level")
            );

            // Trigger training recommendations and auto-advance status
            Main.Beneficiary b = Main.findBeneficiaryById(newId);
            if (b != null) {
                Main.calculateTrainingRecommendations(b);  // compute (side-effect: updates status)
                b.setStatus("Recommendation Generated");
                Main.saveBeneficiaryToDb(b);
            }

            // Mark session complete and log beneficiary_id
            session.status = "completed";
            session.beneficiaryId = newId;
            Main.saveOnboardingSessionToDb(session);
            Main.onboardingSessions.remove(sessionId); // free memory

            sendResponse(exchange, 200, "application/json", String.format(
                "{\"success\":true,\"beneficiaryId\":\"%s\",\"status\":\"Recommendation Generated\"}",
                escapeJson(newId)
            ));
        }
    }

    // ============================================================================
    // AI CHAT HANDLER — POST /api/ai-chat
    // Body: {"message":"...", "beneficiaryId":"optional"}
    // ============================================================================
    static class AiChatHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // CORS pre-flight
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
                return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> params = parseJsonOrForm(body);

            String message = params.getOrDefault("message", "").trim();
            String beneficiaryId = params.getOrDefault("beneficiaryId", "").trim();

            if (message.isEmpty()) {
                sendResponse(exchange, 400, "application/json", "{\"error\":\"message is required\"}");
                return;
            }

            Main.Beneficiary beneficiary = beneficiaryId.isEmpty() ? null : Main.findBeneficiaryById(beneficiaryId);

            String[] result = GeminiEngine.generateResponse(message, beneficiary);
            String reply = result[0];
            String intent = result[1];

            String json = String.format(
                "{\"reply\":\"%s\",\"intent\":\"%s\",\"success\":true}",
                escapeJson(reply), escapeJson(intent)
            );
            sendResponse(exchange, 200, "application/json", json);
        }
    }

    // ============================================================================
    // RECORDING HANDLER — POST /api/recordings (upload), GET /api/recordings?file=...
    // ============================================================================
    static class RecordingHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, GET, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
                exchange.sendResponseHeaders(204, -1);
                return;
            }

            if ("POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                // Save raw audio bytes to recordings/ directory
                String filename = "recording_" + System.currentTimeMillis() + ".webm";
                File dir = new File("recordings");
                dir.mkdirs();
                File outFile = new File(dir, filename);

                try (InputStream is = exchange.getRequestBody();
                     FileOutputStream fos = new FileOutputStream(outFile)) {
                    byte[] buf = new byte[4096];
                    int len;
                    while ((len = is.read(buf)) != -1) fos.write(buf, 0, len);
                }
                sendResponse(exchange, 200, "application/json",
                    String.format("{\"success\":true,\"filename\":\"%s\"}", filename));

            } else if ("GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                Map<String, String> q = queryToMap(exchange.getRequestURI().getQuery());
                String filename = q.get("file");

                if (filename == null || filename.contains("..") || filename.contains("/")) {
                    // List all recordings
                    File dir = new File("recordings");
                    String[] files = dir.exists() ? dir.list() : new String[0];
                    if (files == null) files = new String[0];
                    StringBuilder json = new StringBuilder("[");
                    for (int i = 0; i < files.length; i++) {
                        json.append("\"").append(escapeJson(files[i])).append("\"");
                        if (i < files.length - 1) json.append(",");
                    }
                    json.append("]");
                    sendResponse(exchange, 200, "application/json", json.toString());
                } else {
                    // Serve specific file
                    File audioFile = new File("recordings", filename);
                    if (!audioFile.exists()) {
                        sendResponse(exchange, 404, "application/json", "{\"error\":\"Not found\"}");
                        return;
                    }
                    byte[] bytes = Files.readAllBytes(audioFile.toPath());
                    exchange.getResponseHeaders().set("Content-Type", "audio/webm");
                    exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
                    exchange.sendResponseHeaders(200, bytes.length);
                    try (OutputStream os = exchange.getResponseBody()) { os.write(bytes); }
                }
            } else {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}");
            }
        }
    }

    // ============================================================================
    // AI ENGINE — Rule-based NLP intent detection + response generation
    // ============================================================================
    static class AiEngine {

        // Returns [reply, intent]
        public static String[] generateResponse(String message, Main.Beneficiary beneficiary) {
            String lower = message.toLowerCase();
            String intent = detectIntent(lower);
            String lang = detectLanguage(lower, beneficiary);
            String reply = buildReply(intent, message, beneficiary, lang);
            return new String[]{ reply, intent };
        }

        private static String detectLanguage(String text, Main.Beneficiary b) {
            // Check Unicode script ranges
            for (char c : text.toCharArray()) {
                if (c >= '\u0C00' && c <= '\u0C7F') return "te-IN"; // Telugu
                if (c >= '\u0B80' && c <= '\u0BFF') return "ta-IN"; // Tamil
                if (c >= '\u0900' && c <= '\u097F') return "hi-IN"; // Hindi / Devanagari
                if (c >= '\u0C80' && c <= '\u0CFF') return "kn-IN"; // Kannada
            }
            if (b != null && b.getLanguage() != null) {
                String bl = b.getLanguage().toLowerCase();
                if (bl.contains("telugu")) return "te-IN";
                if (bl.contains("tamil")) return "ta-IN";
                if (bl.contains("hindi")) return "hi-IN";
                if (bl.contains("kannada")) return "kn-IN";
            }
            return "en-IN";
        }

        private static String detectIntent(String lower) {
            if (containsAny(lower, "hello", "hi", "namaste", "vanakkam", "namaskar", "good morning", "good evening", "hey saathi",
                "నమస్కారం", "வணக்கம்", "नमस्ते", "ನಮಸ್ಕಾರ"))
                return "greeting";
            if (containsAny(lower, "training", "course", "programme", "nsqf", "enroll", "learn", "skill course", "pm-daksh",
                "శిక్షణ", "కోర్సు", "பயிற்சி", "பாடநெறி", "ट्रेनिंग", "प्रशिक्षण", "కోర్స్", "ತರಬೇತಿ", "ಕೋರ್ಸ್"))
                return "training_query";
            if (containsAny(lower, "job", "work", "placement", "employment", "livelihood", "opportunity", "vacancy", "salary",
                "ఉద్యోగం", "ఉపాధి", "పని", "வேலை", "வேலைவாய்ப்பு", "தொழில்", "नौकरी", "रोजगार", "काम", "ಉದ್ಯೋಗ", "ಕೆಲಸ"))
                return "job_query";
            if (containsAny(lower, "register", "profile", "sign up", "new beneficiary", "add beneficiary", "create profile",
                "నమోదు", "ప్రొఫైల్", "பதிவு", "பயனாளி", "पंजीकरण", "प्रोफाइल", "ಖಾತೆ", "ನೋಂದಣಿ"))
                return "registration_help";
            if (containsAny(lower, "skill gap", "missing skill", "what skill", "need to learn", "gap analysis",
                "నైపుణ్యం", "திறன்", "कौशल", "ಕೌಶಲ್ಯ"))
                return "skill_gap";
            if (containsAny(lower, "roadmap", "plan", "pathway", "career path", "step", "next step", "future",
                "రోడ్‌మ్యాప్", "திட்டம்", "ரோட்மேப்", "रोडमैप", "योजना", "ಮಾರ್ಗಸೂಚಿ"))
                return "roadmap_help";
            if (containsAny(lower, "status", "update", "pipeline", "progress", "placed", "completed",
                "స్థితి", "நிலை", "स्थिति", "ಪ್ರಸ್ತುತ ಸ್ಥಿತಿ"))
                return "status_query";
            if (containsAny(lower, "help", "support", "assist", "guide", "how", "what can",
                "సహాయం", "உதவி", "मदद", "सहायता", "ಸಹಾಯ"))
                return "general_help";
            if (containsAny(lower, "thank", "thanks", "dhanyawad", "nandri", "shukriya",
                "ధన్యవాదాలు", "நன்றி", "धन्यवाद", "ಶುಕ್ರಿಯಾ", "ಧನ್ಯವಾದಗಳು"))
                return "thanks";
            if (containsAny(lower, "language", "hindi", "tamil", "telugu", "kannada", "english", "regional",
                "భాష", "மொழி", "भाषा", "ಭಾಷೆ"))
                return "language_query";
            if (containsAny(lower, "record", "voice", "audio", "mic", "speak", "call",
                "వాయిస్", "ఫోన్", "குரல்", "அழைப்பு", "कॉल", "आवाज", "ಮಾತು", "ಧ್ವನಿ"))
                return "voice_help";
            if (containsAny(lower, "dashboard", "officer", "monitoring", "report", "analytics",
                "డ్యాష్‌బోర్డ్", "கண்காணிப்பு", "डैशबोर्ड", "ಅಧಿಕಾರಿ"))
                return "dashboard_query";
            return "general";
        }

        private static String buildReply(String intent, String original, Main.Beneficiary b, String lang) {
            String name = b != null ? b.getName() : "Friend";
            String region = b != null ? b.getRegion() : "your region";
            String skills = b != null ? b.getSkills() : "your skills";
            String status = b != null ? b.getStatus() : "unknown";

            // Localized responses for Telugu
            if ("te-IN".equals(lang)) {
                return switch (intent) {
                    case "greeting" -> String.format("నమస్కారం %s! 🙏 నేను సాథి (Saathi), మీ AI జీవనోపాధి సహాయకుడిని. శిక్షణ, ఉద్యోగాలు మరియు రోడ్‌మ్యాప్‌లలో నేను మీకు సహాయం చేయగలను. మీకు ఎలా సహాయపడగలను?", name);
                    case "training_query" -> "📚 మేము డిజిటల్ స్కిల్స్, టైలరింగ్, ఎలక్ట్రికల్, ఆహార ప్రాసెసింగ్ మొదలైన 8 NSQF గుర్తింపు పొందిన శిక్షణా కార్యక్రమాలను అందిస్తున్నాము. 'AI Training Engine' ట్యాబ్‌లో మీ ప్రొఫైల్‌కు తగిన కోర్సులను చూడవచ్చు.";
                    case "job_query" -> String.format("💼 %s ప్రాంతంలో అందుబాటులో ఉన్న వేతన ఉద్యోగాలు మరియు స్వయం ఉపాధి అవకాశాలను 'Job & Placement' ట్యాబ్‌లో పరిశీలించవచ్చు.", region);
                    case "registration_help" -> "📋 కొత్త లబ్ధిదారుని నమోదు చేయడానికి 'Beneficiary Profiler' ట్యాబ్‌ని ఉపయోగించండి లేదా 🎙️ 'Start AI Call' ద్వారా నేరుగా మాట్లాడి నమోదు చేసుకోండి.";
                    case "thanks" -> "చాలా ధన్యవాదాలు! 😊 మీకు సహాయపడటానికి సాథి ఎల్లప్పుడూ సిద్ధంగా ఉంది.";
                    default -> String.format("మీరు '%s' గురించి అడిగారు. శిక్షణ కోర్సులు, ఉద్యోగ అవకాశాలు లేదా లబ్ధిదారుల నమోదు గురించి మీకు సహాయం కావాలా? దయచేసి అడగండి.", original);
                };
            }

            // Localized responses for Tamil
            if ("ta-IN".equals(lang)) {
                return switch (intent) {
                    case "greeting" -> String.format("வணக்கம் %s! 🙏 நான் சாதி (Saathi), உங்கள் AI வாழ்வாதார வழிகாட்டி. பயிற்சி, வேலைவாய்ப்பு மற்றும் தொழில் வரைபடத்தில் உதவ நான் தயாராக உள்ளேன்.", name);
                    case "training_query" -> "📚 தையல், கணினி, மின்சார தொழில்நுட்பம் உள்ளிட்ட 8 NSQF அங்கீகரிக்கப்பட்ட பயிற்சித் திட்டங்கள் உள்ளன. 'AI Training Engine' பகுதியில் பொருத்தமான படிப்புகளைக் காணலாம்.";
                    case "job_query" -> String.format("💼 %s மாவட்டத்தில் உள்ள வேலைவாய்ப்புகளை 'Job & Placement' பகுதியில் பார்க்கலாம்.", region);
                    case "registration_help" -> "📋 புதிய பயனாளியை பதிவு செய்ய 'Beneficiary Profiler' அல்லது 🎙️ 'Start AI Call' நேரடி குரல் அழைப்பைப் பயன்படுத்தவும்.";
                    case "thanks" -> "மிக்க நன்றி! 😊 சாதி எப்போதும் உங்களுக்கு உதவ தயாராக உள்ளது.";
                    default -> String.format("நீங்கள் '%s' பற்றி கேட்கிறீர்கள். பயிற்சி, வேலைவாய்ப்பு அல்லது பயனாளி பதிவு பற்றி எதையும் கேளுங்கள்.", original);
                };
            }

            // Localized responses for Hindi
            if ("hi-IN".equals(lang)) {
                return switch (intent) {
                    case "greeting" -> String.format("नमस्ते %s! 🙏 मैं साथी (Saathi) हूँ, आपका AI आजीविका सहायक। मैं प्रशिक्षण, नौकरी और करियर रोडमैप में आपकी सहायता कर सकता हूँ।", name);
                    case "training_query" -> "📚 हमारे पास सिलाई, डिजिटल कौशल, इलेक्ट्रीशियन, फूड प्रोसेसिंग जैसे 8 NSQF मान्यता प्राप्त कोर्स हैं। 'AI Training Engine' टैब पर अपने लिए सर्वोत्तम कोर्स देखें।";
                    case "job_query" -> String.format("💼 %s क्षेत्र में उपलब्ध स्थानीय नौकरी और स्वरोजगार के अवसरों के लिए 'Job & Placement' टैब देखें।", region);
                    case "registration_help" -> "📋 नए लाभार्थी को पंजीकृत करने के लिए 'Beneficiary Profiler' या 🎙️ 'Start AI Call' का उपयोग करके बोलकर आसानी से फॉर्म भरें।";
                    case "thanks" -> "आपका बहुत-बहुत धन्यवाद! 😊 साथी आपकी सेवा के लिए सदैव तत्पर है।";
                    default -> String.format("आपने '%s' के बारे में पूछा। क्या आप प्रशिक्षण या रोजगार के बारे में अधिक जानना चाहते हैं?", original);
                };
            }

            // Localized responses for Kannada
            if ("kn-IN".equals(lang)) {
                return switch (intent) {
                    case "greeting" -> String.format("ನಮಸ್ಕಾರ %s! 🙏 ನಾನು ಸಾಥಿ (Saathi), ನಿಮ್ಮ AI ಜೀವನೋಪಾಯ ಸಹಾಯಕ. ತರಬೇತಿ, ಉದ್ಯೋಗ ಮತ್ತು ವೃತ್ತಿ ಮಾರ್ಗಸೂಚಿಯಲ್ಲಿ ನಾನು ನಿಮಗೆ ಸಹಾಯ ಮಾಡಬಲ್ಲೆ.", name);
                    case "training_query" -> "📚 ಹೊಲಿಗೆ, ಕಂಪ್ಯೂಟರ್, ಎಲೆಕ್ಟ್ರಿಕಲ್ ಮುಂತಾದ 8 NSQF ಪ್ರಮಾಣೀಕೃತ ತರಬೇತಿ ಕಾರ್ಯಕ್ರಮಗಳು ಲಭ್ಯವಿದೆ. 'AI Training Engine' ಟ್ಯಾಬ್‌ನಲ್ಲಿ ಪರಿಶೀಲಿಸಿ.";
                    case "job_query" -> String.format("💼 %s ಪ್ರದೇಶದಲ್ಲಿ ಲಭ್ಯವಿರುವ ಸ್ಥಳೀಯ ಉದ್ಯೋಗಾವಕಾಶಗಳಿಗಾಗಿ 'Job & Placement' ಟ್ಯಾಬ್ ವೀಕ್ಷಿಸಿ.", region);
                    case "registration_help" -> "📋 ಹೊಸ ಫಲಾನುಭವಿಯನ್ನು ನೋಂದಾಯಿಸಲು 'Beneficiary Profiler' ಅಥವಾ 🎙️ 'Start AI Call' ಧ್ವನಿ ಕರೆಯನ್ನು ಬಳಸಿ.";
                    case "thanks" -> "ತುಂಬಾ ಧನ್ಯವಾದಗಳು! 😊 ಸಾಥಿ ಯಾವಾಗಲೂ ನಿಮ್ಮ ಬೆಂಬಲಕ್ಕೆ ಇರುತ್ತದೆ.";
                    default -> String.format("ನೀವು '%s' ಬಗ್ಗೆ ಕೇಳಿದ್ದೀರಿ. ತರಬೇತಿ ಅಥವಾ ಉದ್ಯೋಗದ ಕುರಿತು ಇನ್ನಷ್ಟು ಮಾಹಿತಿ ಬೇಕೇ?", original);
                };
            }

            // Default English
            return switch (intent) {
                case "greeting" -> String.format(
                    "Namaste %s! 🙏 I am Saathi, your AI-powered multilingual livelihood assistant. " +
                    "I can help you with training recommendations, job matching, skill gap analysis, " +
                    "beneficiary registration, and career roadmaps. How can I assist you today?",
                    name);

                case "training_query" -> {
                    String extra = b != null
                        ? String.format(" Based on your skills (%s) and region (%s), I suggest visiting the \"AI Training Engine\" tab to get personalized NSQF-aligned training recommendations.", skills, region)
                        : " We have 8 NSQF-aligned training programmes including Digital Skills, Tailoring, Electrical Technician, Food Processing, Computer Hardware, Beauty & Wellness, Organic Farming, and Handicraft courses.";
                    yield "📚 Great question about training!" + extra +
                          " Training programmes range from 3 to 6 months, with linkages to PM-DAKSH and NBCFDC skill portals.";
                }

                case "job_query" -> {
                    String extra = b != null
                        ? String.format(" For %s in %s, the \"Job & Placement\" tab will show you matched local opportunities based on your profile.", name, region)
                        : " Available opportunities include Data Entry, Tailoring, Electrical Service, Food Processing SHG, IT Support, Artisan Cooperatives, and Organic Farming.";
                    yield "💼 Looking for livelihood opportunities?" + extra +
                          " Both wage employment and self-employment options are available, tailored to your region.";
                }

                case "registration_help" -> "📋 To register a new beneficiary, navigate to the \"Beneficiary Profiler\" tab or use the 🎙️ \"Start AI Call\" on the Dashboard to register seamlessly via voice!";

                case "skill_gap" -> {
                    String extra = b != null
                        ? String.format(" Your current skills are: %s. Open the \"Livelihood Roadmap\" tab to see your personal skill gap analysis against recommended NSQF programmes.", skills)
                        : " Skill gap analysis compares your existing skills against required skills for top recommended training programmes.";
                    yield "🔍 Skill Gap Analysis identifies what skills you need to develop." + extra;
                }

                case "roadmap_help" -> {
                    String extra = b != null
                        ? String.format(" %s, your current pipeline status is: %s. Open the \"Livelihood Roadmap\" tab to see your personalized 6-step career pathway.", name, status)
                        : " The roadmap has 3 key stages: Baseline Skill Gap Analysis → NSQF Training & Certification → Placement & Enterprise Linkage.";
                    yield "🗺️ The personalized livelihood roadmap charts your career journey!" + extra;
                }

                case "status_query" -> {
                    String extra = b != null
                        ? String.format(" %s's current status is: %s. Statuses progress from 'Profile Created' → 'Recommendation Generated' → 'Enrolled' → 'Training In Progress' → 'Training Completed' → 'Placed'.", name, status)
                        : " Pipeline statuses track each beneficiary's journey from registration to placement.";
                    yield "📊 Pipeline Status Tracking!" + extra +
                          " Use the Dashboard's ✏️ Status button to update a beneficiary's status.";
                }

                case "general_help" -> """
                    🤖 Here's what I can help you with:
                    • 📋 Beneficiary registration & profile management
                    • 🎓 AI Training recommendations (NSQF aligned)
                    • 💼 Job & livelihood opportunity matching
                    • 🔍 Skill gap analysis & personalized roadmap
                    • 📊 Officer dashboard & pipeline monitoring
                    • 🎤 Interactive Voice agent in Telugu, Tamil, Hindi, Kannada & English
                    Just ask me anything about livelihood assistance!""";

                case "thanks" -> String.format(
                    "You're most welcome, %s! 😊 Saathi is always here to support your livelihood journey. " +
                    "Remember, you can speak to me using the Voice Message or Start AI Call buttons.", name);

                case "language_query" -> "🌐 Saathi supports 5 Indian languages: Telugu (తెలుగు), Tamil (தமிழ்), Hindi (हिंदी), Kannada (ಕನ್ನಡ), and English. Select your preferred language anytime from the top bar!";

                case "voice_help" -> """
                    🎤 Saathi's Voice Communication Hub offers:
                    • 🎙️ Assisted Voice Call — Full AI turn-taking phone conversation in browser
                    • 🔴 Push-to-Talk Registration — Guided step-by-step recording
                    • 💬 AI Voice Chat — Type or speak questions with instant audio responses""";

                case "dashboard_query" -> "📊 The Officer Dashboard shows real-time pipeline metrics: total beneficiaries, enrolled in training, placed/self-employed, and those needing officer support.";

                default -> {
                    String contextInfo = b != null
                        ? String.format(" I have %s's profile (Region: %s, Skills: %s, Status: %s).", name, region, skills, status)
                        : " Please select a beneficiary if you need profile-specific guidance.";
                    yield String.format(
                        "I understand you're asking about: \"%s\".%s " +
                        "I'm here to help with training, job matching, skill gaps, beneficiary registration, " +
                        "and livelihood roadmaps. Try asking: 'What training is available?' or 'How to register a beneficiary?'.",
                        original.length() > 60 ? original.substring(0, 60) + "..." : original, contextInfo);
                }
            };
        }

        private static boolean containsAny(String text, String... keywords) {
            for (String kw : keywords) {
                if (text.contains(kw)) return true;
            }
            return false;
        }
    }

    // ============================================================================
    // NEW: POST /api/onboarding/session
    // Unified endpoint — starts a new session OR advances an existing one.
    // Body (start):   {"mode":"voice","language":"hi-IN"}
    // Body (advance): {"sessionId":"SES...", "answer":"Ravi Kumar"}
    // ============================================================================
    static class OnboardingSessionHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { exchange.sendResponseHeaders(204, -1); return; }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}"); return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> p = parseJsonOrForm(body);

            String sessionId = p.getOrDefault("sessionId", "").trim();
            String answer    = p.getOrDefault("answer", "").trim();

            // ── Branch: no sessionId → START a new session ────────────────────
            if (sessionId.isEmpty()) {
                String mode     = p.getOrDefault("mode", "direct").trim();
                String language = p.getOrDefault("language", "en-IN").trim();

                sessionId = "SES" + System.currentTimeMillis() + (int)(Math.random() * 1000);
                Main.BeneficiaryOnboardingSession session =
                    new Main.BeneficiaryOnboardingSession(sessionId, mode, language);

                Main.onboardingSessions.put(sessionId, session);
                Main.saveOnboardingSessionToDb(session);

                String welcome     = Main.LocalizedPrompts.getWelcome(language);
                String step1Prompt = Main.LocalizedPrompts.getPrompt(language, 1);
                String ttsHint     = Main.TextToSpeechService.synthesize(welcome + " " + step1Prompt, language);

                sendResponse(exchange, 200, "application/json", String.format(
                    "{\"sessionId\":\"%s\",\"mode\":\"%s\",\"language\":\"%s\",\"step\":1," +
                    "\"totalSteps\":10,\"prompt\":\"%s\",\"field\":\"name\",\"done\":false,\"tts\":%s}",
                    escapeJson(sessionId), escapeJson(mode), escapeJson(language),
                    escapeJson(welcome + " " + step1Prompt), ttsHint
                ));
                return;
            }

            // ── Branch: sessionId present → ADVANCE the session ───────────────
            Main.BeneficiaryOnboardingSession session = Main.onboardingSessions.get(sessionId);
            if (session == null) {
                sendResponse(exchange, 404, "application/json",
                    "{\"error\":\"Session not found. Please omit sessionId to start a new session.\"}");
                return;
            }

            if (session.isComplete()) {
                sendResponse(exchange, 200, "application/json", String.format(
                    "{\"sessionId\":\"%s\",\"done\":true,\"step\":11,\"totalSteps\":10," +
                    "\"message\":\"Session already complete. Call /api/onboarding/complete to register.\"}",
                    escapeJson(sessionId)));
                return;
            }

            int    currentStep = session.currentStep;
            String fieldKey    = session.currentFieldKey();
            String lang        = session.language;

            // Validate / extract the answer
            String extracted = Main.ConversationalExtractionService.extract(currentStep, answer);
            if (extracted == null) {
                String retryPrefix = switch (lang) {
                    case "hi-IN" -> "क्षमा करें, वह सही नहीं लगा। कृपया फिर से प्रयास करें। ";
                    case "ta-IN" -> "மன்னிக்கவும், மீண்டும் முயற்சிக்கவும். ";
                    case "te-IN" -> "క్షమించండి, దయచేసి మళ్ళీ ప్రయత్నించండి. ";
                    case "kn-IN" -> "ಕ್ಷಮಿಸಿ, ದಯವಿಟ್ಟು ಮತ್ತೆ ಪ್ರಯತ್ನಿಸಿ. ";
                    default      -> "Sorry, that doesn't look right. Please try again. ";
                };
                String retryPrompt = retryPrefix + Main.LocalizedPrompts.getPrompt(lang, currentStep);
                String tts = Main.TextToSpeechService.synthesize(retryPrompt, lang);
                sendResponse(exchange, 200, "application/json", String.format(
                    "{\"sessionId\":\"%s\",\"step\":%d,\"totalSteps\":10,\"field\":\"%s\"," +
                    "\"done\":false,\"valid\":false,\"nextPrompt\":\"%s\",\"tts\":%s}",
                    escapeJson(sessionId), currentStep, escapeJson(fieldKey),
                    escapeJson(retryPrompt), tts
                ));
                return;
            }

            session.submitAndAdvance(extracted);
            Main.saveOnboardingSessionToDb(session);

            boolean done = session.isComplete();
            String nextPrompt = done
                ? Main.LocalizedPrompts.getWelcome(lang).contains("Welcome")
                    ? "Excellent! All 10 questions complete. Your profile is being saved."
                    : "सभी प्रश्न पूरे हुए। प्रोफ़ाइल सहेजी जा रही है।"
                : Main.LocalizedPrompts.getPrompt(lang, session.currentStep);
            String tts = Main.TextToSpeechService.synthesize(nextPrompt, lang);
            String nextField = done ? "complete" : (session.currentFieldKey() != null ? session.currentFieldKey() : "complete");

            sendResponse(exchange, 200, "application/json", String.format(
                "{\"sessionId\":\"%s\",\"step\":%d,\"totalSteps\":10,\"field\":\"%s\"," +
                "\"value\":\"%s\",\"done\":%b,\"valid\":true,\"nextPrompt\":\"%s\"," +
                "\"nextField\":\"%s\",\"tts\":%s}",
                escapeJson(sessionId), currentStep, escapeJson(fieldKey),
                escapeJson(extracted), done, escapeJson(nextPrompt),
                escapeJson(nextField), tts
            ));
        }
    }

    // ============================================================================
    // NEW: POST /api/beneficiary/direct
    // Direct registration from AI voice tool call or manual form.
    // Body JSON: {name, phone, education, familyOccupation, currentLivelihood,
    //             skills, interests, aspirations, constraints,
    //             employmentPreference, region, language?}
    // ============================================================================
    static class BeneficiaryDirectHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { exchange.sendResponseHeaders(204, -1); return; }
            if (!"POST".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}"); return;
            }

            String body = readRequestBody(exchange);
            Map<String, String> p = parseJsonOrForm(body);

            String name    = p.getOrDefault("name", "").trim();
            String phone   = p.getOrDefault("phone", "").trim();
            if (name.isEmpty() || phone.isEmpty()) {
                sendResponse(exchange, 400, "application/json",
                    "{\"error\":\"Fields 'name' and 'phone' are required\"}");
                return;
            }

            String lang    = p.getOrDefault("language", "English").trim();
            String edu     = p.getOrDefault("education", "Not Specified");
            String famOcc  = p.getOrDefault("familyOccupation", "None");
            String curLiv  = p.getOrDefault("currentLivelihood", "Unemployed");
            String skills  = p.getOrDefault("skills", "Basic Communication");
            String inter   = p.getOrDefault("interests", "General Employment");
            String aspir   = p.getOrDefault("aspirations", "Gainful Livelihood");
            String constr  = p.getOrDefault("constraints", "None");
            String empPref = p.getOrDefault("employmentPreference", "Both");
            String region  = p.getOrDefault("region", "District Level");

            String newId = Main.registerBeneficiaryDirect(
                name, phone, lang, edu, famOcc, curLiv, skills, inter, aspir, constr, empPref, region
            );

            // Auto-advance status to Recommendation Generated
            Main.Beneficiary b = Main.findBeneficiaryById(newId);
            if (b != null) {
                Main.calculateTrainingRecommendations(b);
                b.setStatus("Recommendation Generated");
                Main.saveBeneficiaryToDb(b);
            }

            System.out.println("[API] POST /api/beneficiary/direct → registered id=" + newId + " name=" + name);

            sendResponse(exchange, 201, "application/json", String.format(
                "{\"success\":true,\"id\":\"%s\",\"name\":\"%s\",\"status\":\"Recommendation Generated\"}",
                escapeJson(newId), escapeJson(name)
            ));
        }
    }

    // ============================================================================
    // NEW: BeneficiaryRouterHandler — dispatches /api/beneficiary/* paths
    //   GET  /api/beneficiary/{id}/roadmap  → full roadmap JSON
    //   (all other sub-paths fall through to existing BeneficiaryDetailHandler logic)
    // ============================================================================
    static class BeneficiaryRouterHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { exchange.sendResponseHeaders(204, -1); return; }

            String path = exchange.getRequestURI().getPath(); // e.g. /api/beneficiary/1001/roadmap
            // Strip prefix "/api/beneficiary/"
            String remainder = path.replaceFirst("^/api/beneficiary/?", "");
            String[] segments = remainder.split("/");

            // Route: /{id}/roadmap
            if (segments.length >= 2 && "roadmap".equalsIgnoreCase(segments[1])) {
                handleRoadmap(exchange, segments[0]);
                return;
            }

            // Route: /direct — handled by BeneficiaryDirectHandler (already registered first)
            // This router should never receive /direct due to context ordering, but guard anyway
            if (segments.length >= 1 && "direct".equalsIgnoreCase(segments[0])) {
                new BeneficiaryDirectHandler().handle(exchange);
                return;
            }

            // Fallback — 404
            sendResponse(exchange, 404, "application/json",
                "{\"error\":\"Unknown /api/beneficiary/* sub-path: " + escapeJson(path) + "\"}");
        }

        private void handleRoadmap(HttpExchange exchange, String id) throws IOException {
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}"); return;
            }

            Main.Beneficiary b = Main.findBeneficiaryById(id);
            if (b == null) {
                sendResponse(exchange, 404, "application/json",
                    "{\"error\":\"Beneficiary not found\",\"id\":\"" + escapeJson(id) + "\"}");
                return;
            }

            List<Main.Recommendation<Main.TrainingProgram>> trainings  = Main.calculateTrainingRecommendations(b);
            List<Main.Recommendation<Main.Opportunity>>     opps       = Main.calculateOpportunityRecommendations(b);

            Main.TrainingProgram topTP  = trainings.isEmpty()  ? Main.getTrainingPrograms().get(0) : trainings.get(0).getItem();
            Main.Opportunity     topOpp = opps.isEmpty()       ? Main.getOpportunities().get(0)    : opps.get(0).getItem();

            List<String> skillGaps = Main.performSkillGapAnalysis(b.getSkills(), topTP.getSkillsKeywords());

            // Build training recommendations array
            StringBuilder trainingArr = new StringBuilder("[");
            for (int i = 0; i < Math.min(trainings.size(), 5); i++) {
                Main.Recommendation<Main.TrainingProgram> r = trainings.get(i);
                Main.TrainingProgram tp = r.getItem();
                trainingArr.append("{")
                    .append("\"id\":\"").append(escapeJson(tp.getTrainingId())).append("\",")
                    .append("\"name\":\"").append(escapeJson(tp.getProgramName())).append("\",")
                    .append("\"nsqfLevel\":").append(tp.getNsqfLevel()).append(",")
                    .append("\"region\":\"").append(escapeJson(tp.getRegion())).append("\",")
                    .append("\"employmentType\":\"").append(escapeJson(tp.getEmploymentType())).append("\",")
                    .append("\"duration\":\"").append(escapeJson(tp.getDuration())).append("\",")
                    .append("\"skillsKeywords\":\"").append(escapeJson(tp.getSkillsKeywords())).append("\",")
                    .append("\"description\":\"").append(escapeJson(tp.getDescription())).append("\",")
                    .append("\"score\":").append(r.getScore()).append(",")
                    .append("\"reasons\":[");
                List<String> reasons = r.getReasons();
                for (int j = 0; j < reasons.size(); j++) {
                    trainingArr.append("\"").append(escapeJson(reasons.get(j))).append("\"");
                    if (j < reasons.size() - 1) trainingArr.append(",");
                }
                trainingArr.append("]}");
                if (i < Math.min(trainings.size(), 5) - 1) trainingArr.append(",");
            }
            trainingArr.append("]");

            // Build opportunities array
            StringBuilder oppArr = new StringBuilder("[");
            for (int i = 0; i < Math.min(opps.size(), 5); i++) {
                Main.Recommendation<Main.Opportunity> r = opps.get(i);
                Main.Opportunity opp = r.getItem();
                oppArr.append("{")
                    .append("\"id\":\"").append(escapeJson(opp.getOpportunityId())).append("\",")
                    .append("\"name\":\"").append(escapeJson(opp.getOpportunityName())).append("\",")
                    .append("\"region\":\"").append(escapeJson(opp.getRegion())).append("\",")
                    .append("\"type\":\"").append(escapeJson(opp.getType())).append("\",")
                    .append("\"requiredSkill\":\"").append(escapeJson(opp.getRequiredSkill())).append("\",")
                    .append("\"description\":\"").append(escapeJson(opp.getDescription())).append("\",")
                    .append("\"score\":").append(r.getScore()).append("}");
                if (i < Math.min(opps.size(), 5) - 1) oppArr.append(",");
            }
            oppArr.append("]");

            // Build skill gaps array
            StringBuilder gapsArr = new StringBuilder("[");
            for (int i = 0; i < skillGaps.size(); i++) {
                gapsArr.append("\"").append(escapeJson(skillGaps.get(i))).append("\"");
                if (i < skillGaps.size() - 1) gapsArr.append(",");
            }
            gapsArr.append("]");

            String json = String.format(
                "{" +
                "\"beneficiaryId\":\"%s\"," +
                "\"name\":\"%s\"," +
                "\"status\":\"%s\"," +
                "\"region\":\"%s\"," +
                "\"existingSkills\":\"%s\"," +
                "\"targetProgram\":{" +
                    "\"id\":\"%s\"," +
                    "\"name\":\"%s\"," +
                    "\"nsqfLevel\":%d," +
                    "\"duration\":\"%s\"," +
                    "\"requiredSkills\":\"%s\"" +
                "}," +
                "\"skillGaps\":%s," +
                "\"targetRole\":{" +
                    "\"name\":\"%s\"," +
                    "\"type\":\"%s\"," +
                    "\"requiredSkill\":\"%s\"," +
                    "\"region\":\"%s\"" +
                "}," +
                "\"trainingRecommendations\":%s," +
                "\"opportunityRecommendations\":%s," +
                "\"roadmapSteps\":[" +
                    "{\"step\":1,\"title\":\"Current Skills Profile\",\"detail\":\"%s\"}," +
                    "{\"step\":2,\"title\":\"Skill Gap Analysis\",\"detail\":\"Gaps: %s\"}," +
                    "{\"step\":3,\"title\":\"Recommended NSQF Training\",\"detail\":\"%s (NSQF L%d)\"}," +
                    "{\"step\":4,\"title\":\"Training Completion & Certification\",\"detail\":\"Obtain NSQF Skill Certification\"}," +
                    "{\"step\":5,\"title\":\"Target Livelihood / Job Placement\",\"detail\":\"%s (%s)\"}," +
                    "{\"step\":6,\"title\":\"Sustainable Next Steps\",\"detail\":\"Linkage to micro-credit & District Welfare Officer monitoring\"}" +
                "]" +
                "}",
                escapeJson(b.getId()), escapeJson(b.getName()), escapeJson(b.getStatus()),
                escapeJson(b.getRegion()), escapeJson(b.getSkills()),
                escapeJson(topTP.getTrainingId()), escapeJson(topTP.getProgramName()),
                topTP.getNsqfLevel(), escapeJson(topTP.getDuration()), escapeJson(topTP.getSkillsKeywords()),
                gapsArr.toString(),
                escapeJson(topOpp.getOpportunityName()), escapeJson(topOpp.getType()),
                escapeJson(topOpp.getRequiredSkill()), escapeJson(topOpp.getRegion()),
                trainingArr.toString(),
                oppArr.toString(),
                // roadmap step details
                escapeJson(b.getSkills()),
                escapeJson(skillGaps.isEmpty() ? "None — ready for direct placement" : String.join(", ", skillGaps)),
                escapeJson(topTP.getProgramName()), topTP.getNsqfLevel(),
                escapeJson(topOpp.getOpportunityName()), escapeJson(topOpp.getType())
            );

            sendResponse(exchange, 200, "application/json", json);
        }
    }

    // ============================================================================
    // NEW: GET /api/officer/metrics
    // Returns pipeline counts AND a full beneficiary pipeline array for dashboard.
    // ============================================================================
    static class OfficerMetricsHandler implements HttpHandler {
        @Override
        public void handle(HttpExchange exchange) throws IOException {
            exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
            exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, OPTIONS");
            exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");
            if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) { exchange.sendResponseHeaders(204, -1); return; }
            if (!"GET".equalsIgnoreCase(exchange.getRequestMethod())) {
                sendResponse(exchange, 405, "application/json", "{\"error\":\"Method not allowed\"}"); return;
            }

            int total = Main.getBeneficiaries().size();
            int profilesCreated = 0, recGenerated = 0, enrolled = 0, inProgress = 0,
                completed = 0, placed = 0, selfEmployed = 0, needsSupport = 0;

            for (Main.Beneficiary b : Main.getBeneficiaries()) {
                String s = b.getStatus().toLowerCase();
                if      (s.contains("profile created"))       profilesCreated++;
                else if (s.contains("recommendation"))        recGenerated++;
                else if (s.contains("enrolled"))              enrolled++;
                else if (s.contains("in progress"))           inProgress++;
                else if (s.contains("completed"))             completed++;
                else if (s.contains("placed"))                placed++;
                else if (s.contains("self-employment"))       selfEmployed++;
                else if (s.contains("needs officer support")) needsSupport++;
            }

            // Build pipeline array
            StringBuilder pipeline = new StringBuilder("[");
            List<Main.Beneficiary> list = Main.getBeneficiaries();
            for (int i = 0; i < list.size(); i++) {
                Main.Beneficiary b = list.get(i);
                pipeline.append(String.format(
                    "{\"id\":\"%s\",\"name\":\"%s\",\"phone\":\"%s\"," +
                    "\"region\":\"%s\",\"language\":\"%s\"," +
                    "\"status\":\"%s\",\"registrationDate\":\"%s\"}",
                    escapeJson(b.getId()), escapeJson(b.getName()), escapeJson(b.getPhone()),
                    escapeJson(b.getRegion()), escapeJson(b.getLanguage()),
                    escapeJson(b.getStatus()), escapeJson(b.getRegistrationDate())
                ));
                if (i < list.size() - 1) pipeline.append(",");
            }
            pipeline.append("]");

            String json = String.format(
                "{" +
                "\"counts\":{" +
                    "\"total\":%d," +
                    "\"profilesCreated\":%d," +
                    "\"recGenerated\":%d," +
                    "\"enrolled\":%d," +
                    "\"inProgress\":%d," +
                    "\"completed\":%d," +
                    "\"placed\":%d," +
                    "\"selfEmployed\":%d," +
                    "\"needsSupport\":%d" +
                "}," +
                "\"pipeline\":%s" +
                "}",
                total, profilesCreated, recGenerated, enrolled, inProgress,
                completed, placed, selfEmployed, needsSupport,
                pipeline.toString()
            );

            sendResponse(exchange, 200, "application/json", json);
        }
    }

    // ============================================================================
    // GEMINI AI ENGINE — Google Gemini API-powered intelligent chat
    // Falls back to rule-based AiEngine if GEMINI_API_KEY is not set.
    // ============================================================================
    static class GeminiEngine {

        private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

        private static final java.net.http.HttpClient HTTP_CLIENT =
            java.net.http.HttpClient.newBuilder()
                .connectTimeout(java.time.Duration.ofSeconds(10))
                .build();

        /** Returns [reply, intent] */
        public static String[] generateResponse(String userMessage, Main.Beneficiary beneficiary) {
            String apiKey = System.getenv("GEMINI_API_KEY");
            if (apiKey == null || apiKey.isBlank()) {
                System.out.println("[Gemini] GEMINI_API_KEY not set — using rule-based fallback.");
                return AiEngine.generateResponse(userMessage, beneficiary);
            }

            try {
                String systemPrompt = buildSystemPrompt(beneficiary);
                String requestBody = buildRequestBody(systemPrompt, userMessage);

                java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(URI.create(GEMINI_API_URL + "?key=" + apiKey))
                    .header("Content-Type", "application/json; charset=utf-8")
                    .timeout(java.time.Duration.ofSeconds(25))
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(requestBody, StandardCharsets.UTF_8))
                    .build();

                java.net.http.HttpResponse<String> response =
                    HTTP_CLIENT.send(request, java.net.http.HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                if (response.statusCode() == 200) {
                    String reply = extractText(response.body());
                    if (reply != null && !reply.isBlank()) {
                        System.out.println("[Gemini] ✓ Reply generated (" + reply.length() + " chars)");
                        return new String[]{ reply, "gemini_ai" };
                    }
                } else {
                    System.err.println("[Gemini] API error HTTP " + response.statusCode());
                }
            } catch (java.io.IOException | InterruptedException e) {
                System.err.println("[Gemini] Request failed: " + e.getMessage());
                Thread.currentThread().interrupt();
            }

            // Fallback
            return AiEngine.generateResponse(userMessage, beneficiary);
        }

        private static String buildSystemPrompt(Main.Beneficiary b) {
            StringBuilder s = new StringBuilder();
            s.append("You are Saathi, an AI-powered multilingual livelihood assistant for India's Smart India Hackathon. ");
            s.append("You help SC/ST beneficiaries under PM-DAKSH and NBCFDC schemes access skill training, jobs, and career roadmaps.\n\n");
            s.append("TRAINING PROGRAMMES: Digital Skills & Data Entry (NSQF L4, 3mo, All regions), ");
            s.append("Tailoring & Garment (NSQF L3, 4mo, Rural), Electrical Technician & Solar (NSQF L4, 6mo, Urban), ");
            s.append("Food Processing (NSQF L3, 3mo, Rural), Computer Hardware (NSQF L4, 6mo, Urban), ");
            s.append("Beauty & Wellness (NSQF L3, 3mo, All), Organic Farming (NSQF L4, 4mo, Rural), Handicraft & Artisan (NSQF L3, 3mo, Rural).\n");
            s.append("JOB OPPORTUNITIES: Data Entry Operator, Tailoring Micro-Enterprise, Electrical Service Assistant, ");
            s.append("Food Processing SHG Unit, IT Support Assistant, Local Artisan Cooperative, Organic Agri-Produce Producer.\n\n");
            if (b != null) {
                s.append("BENEFICIARY PROFILE: Name=").append(b.getName())
                 .append(", Region=").append(b.getRegion())
                 .append(", Lang=").append(b.getLanguage())
                 .append(", Education=").append(b.getEducation())
                 .append(", Skills=").append(b.getSkills())
                 .append(", Interests=").append(b.getInterests())
                 .append(", Aspirations=").append(b.getAspirations())
                 .append(", Employment Pref=").append(b.getEmploymentPreference())
                 .append(", Status=").append(b.getStatus()).append(".\n\n");
            }
            s.append("INSTRUCTIONS: Reply warmly and concisely (3-4 sentences max). Use the user's language. ");
            s.append("Use relevant emojis. Reference the beneficiary profile when available. ");
            s.append("Guide users to the correct app tab: Dashboard, Beneficiary Profiler, AI Training Engine, Job & Placement, Livelihood Roadmap, AI Voice Hub.");
            return s.toString();
        }

        private static String buildRequestBody(String systemPrompt, String userMessage) {
            return "{\"system_instruction\":{\"parts\":[{\"text\":\"" + je(systemPrompt) + "\"}]},"
                + "\"contents\":[{\"role\":\"user\",\"parts\":[{\"text\":\"" + je(userMessage) + "\"}]}],"
                + "\"generationConfig\":{\"maxOutputTokens\":400,\"temperature\":0.75}}";
        }

        /** Extract the first text value from a Gemini JSON response */
        private static String extractText(String json) {
            if (json == null) return null;
            int ti = json.indexOf("\"text\":");
            if (ti < 0) return null;
            int start = json.indexOf('"', ti + 7) + 1;
            if (start <= 0) return null;
            StringBuilder result = new StringBuilder();
            boolean esc = false;
            for (int i = start; i < json.length(); i++) {
                char c = json.charAt(i);
                if (esc) {
                    switch (c) {
                        case '"' -> result.append('"');
                        case '\\' -> result.append('\\');
                        case 'n' -> result.append('\n');
                        case 'r' -> result.append('\r');
                        case 't' -> result.append('\t');
                        default  -> { result.append('\\'); result.append(c); }
                    }
                    esc = false;
                } else if (c == '\\') {
                    esc = true;
                } else if (c == '"') {
                    break;
                } else {
                    result.append(c);
                }
            }
            return result.toString();
        }

        /** JSON-escape a string */
        private static String je(String s) {
            if (s == null) return "";
            return s.replace("\\", "\\\\").replace("\"", "\\\"")
                    .replace("\n", "\\n").replace("\r", "\\r").replace("\t", "\\t");
        }
    }
}
