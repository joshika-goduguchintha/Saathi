import java.io.*;
import java.sql.*;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * ============================================================================
 * Saathi – Multilingual Voice Livelihood Assistant
 * Prototype for Smart India Hackathon (SIH)
 *
 * Single-file implementation containing all data models, recommendation engines,
 * skill gap analysis, personalized roadmap generator, officer dashboard, and storage.
 * ============================================================================
 */
public class Main {

    // Global in-memory data repositories
    private static final List<Beneficiary> beneficiaries = new ArrayList<>();
    private static final List<TrainingProgram> trainingPrograms = new ArrayList<>();
    private static final List<Opportunity> opportunities = new ArrayList<>();

    // ID Generator counter — volatile for thread-safe access across onboarding handler threads
    private static volatile int nextBeneficiaryId = 1001;

    // In-memory onboarding session store (sessionId → session)
    static final java.util.concurrent.ConcurrentHashMap<String, BeneficiaryOnboardingSession>
        onboardingSessions = new java.util.concurrent.ConcurrentHashMap<>();

    // Database URL for SQLite persistence
    private static final String DB_URL = "jdbc:sqlite:saathi.db";

    // Database Connection & Initialization Helper
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(DB_URL);
    }

    public static List<Beneficiary> getBeneficiaries() { return beneficiaries; }
    public static List<TrainingProgram> getTrainingPrograms() { return trainingPrograms; }
    public static List<Opportunity> getOpportunities() { return opportunities; }

    public static String registerBeneficiaryDirect(String name, String phone, String language, String education,
                                                  String familyOccupation, String currentLivelihood, String skills,
                                                  String interests, String aspirations, String constraints,
                                                  String employmentPreference, String region) {
        String idStr = String.valueOf(nextBeneficiaryId++);
        String regDate = new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());

        Beneficiary beneficiary = new Beneficiary(
                idStr, name, phone, language, education, familyOccupation,
                currentLivelihood, skills, interests, aspirations, constraints,
                employmentPreference, region, "Profile Created", regDate
        );

        beneficiaries.add(beneficiary);
        saveBeneficiaryToDb(beneficiary);
        return idStr;
    }

    public static void initDatabase() {
        try (Connection conn = getConnection(); Statement stmt = conn.createStatement()) {
            // Table for Beneficiaries
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS beneficiaries (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    phone TEXT,
                    language TEXT,
                    education TEXT,
                    family_occupation TEXT,
                    current_livelihood TEXT,
                    skills TEXT,
                    interests TEXT,
                    aspirations TEXT,
                    constraints TEXT,
                    employment_preference TEXT,
                    region TEXT,
                    status TEXT,
                    registration_date TEXT
                );
            """);

            // Table for Training Programs
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS training_programs (
                    training_id TEXT PRIMARY KEY,
                    program_name TEXT NOT NULL,
                    nsqf_level INTEGER,
                    region TEXT,
                    employment_type TEXT,
                    skills_keywords TEXT,
                    duration TEXT,
                    description TEXT
                );
            """);

            // Table for Opportunities
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS opportunities (
                    opportunity_id TEXT PRIMARY KEY,
                    opportunity_name TEXT NOT NULL,
                    region TEXT,
                    type TEXT,
                    required_skill TEXT,
                    description TEXT
                );
            """);

            // Table for Onboarding Sessions (conversational intake log)
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS onboarding_sessions (
                    session_id       TEXT PRIMARY KEY,
                    mode             TEXT,
                    language         TEXT,
                    current_step     INTEGER,
                    collected_json   TEXT,
                    status           TEXT,
                    beneficiary_id   TEXT,
                    created_at       TEXT,
                    updated_at       TEXT
                );
            """);

            System.out.println("[✓] SQLite Database 'saathi.db' initialized successfully.");
        } catch (SQLException e) {
            System.err.println("[X] SQLite Database initialization error: " + e.getMessage());
        }
    }

    // ========================================================================
    // MAIN ENTRY POINT & CONSOLE MENU LOOP
    // ========================================================================
    public static void main(String[] args) {
        // Initialize SQLite database schema
        initDatabase();

        // Load sample training programmes & local opportunities from DB
        loadSampleData();

        // Load previously saved beneficiary data from SQLite
        loadData();

        if (args.length > 0 && args[0].equalsIgnoreCase("--cli")) {
            runConsoleMenu();
        } else {
            WebServer.main(args);
        }
    }

    private static void runConsoleMenu() {
        try (Scanner scanner = new Scanner(System.in)) {
            boolean running = true;
            while (running) {
                printMainMenu();
                System.out.print("Enter your choice (1-12): ");
                String input = scanner.nextLine().trim();

                int choice;
                try {
                    choice = Integer.parseInt(input);
                } catch (NumberFormatException e) {
                    System.out.println("\n[X] Invalid input! Please enter a valid number between 1 and 12.\n");
                    continue;
                }

                switch (choice) {
                    case 1 -> registerBeneficiary(scanner);
                    case 2 -> viewBeneficiaryProfile(scanner);
                    case 3 -> generateTrainingRecommendations(scanner);
                    case 4 -> generateJobRecommendations(scanner);
                    case 5 -> viewPersonalizedRoadmap(scanner);
                    case 6 -> updateTrainingPlacementStatus(scanner);
                    case 7 -> officerDashboard(scanner);
                    case 8 -> viewTrainingPrograms();
                    case 9 -> viewLocalOpportunities();
                    case 10 -> searchBeneficiary(scanner);
                    case 11 -> saveData();
                    case 12 -> {
                        System.out.println("\nSaving data before exit...");
                        saveData();
                        System.out.println("Thank you for using Saathi! Empowering SC Beneficiaries nationwide.\n");
                        running = false;
                    }
                    default -> System.out.println("\n[X] Invalid choice! Please select an option from 1 to 12.\n");
                }
            }
        }
    }

    private static void printMainMenu() {
        System.out.println("=================================================");
        System.out.println("                   Saathi                     ");
        System.out.println("   AI-Powered Multilingual Livelihood Assistant  ");
        System.out.println("=================================================");
        System.out.println(" 1. Register Beneficiary (Conversational Flow)");
        System.out.println(" 2. View Beneficiary Profile");
        System.out.println(" 3. Generate AI Training Recommendations");
        System.out.println(" 4. Generate Job/Livelihood Recommendations");
        System.out.println(" 5. View Personalized Livelihood Roadmap");
        System.out.println(" 6. Update Training/Placement Status");
        System.out.println(" 7. Officer Dashboard (Monitoring & Pipeline)");
        System.out.println(" 8. View Training Programmes (NSQF Aligned)");
        System.out.println(" 9. View Local Opportunities");
        System.out.println("10. Search Beneficiary");
        System.out.println("11. Save Data");
        System.out.println("12. Exit");
        System.out.println("=================================================");
    }

    // ========================================================================
    // 1. BENEFICIARY REGISTRATION (Conversational Profiling)
    // ========================================================================
    private static void registerBeneficiary(Scanner scanner) {
        System.out.println("\n-------------------------------------------------");
        System.out.println("         BENEFICIARY REGISTRATION PROFILING      ");
        System.out.println("-------------------------------------------------");

        /*
         * MULTILINGUAL & VOICE SIMULATION NOTE:
         * In full production, this module connects to ASR (Speech-to-Text),
         * IVR phone systems, and WhatsApp voice notes in regional dialects.
         */
        System.out.println("Select Interaction Language:");
        System.out.println("1. English");
        System.out.println("2. Hindi (हिंदी)");
        System.out.println("3. Tamil (தமிழ்)");
        System.out.println("4. Telugu (తెలుగు)");
        System.out.print("Choice (1-4, default=1): ");
        String langChoice = scanner.nextLine().trim();

        String selectedLang = switch (langChoice) {
            case "2" -> "Hindi";
            case "3" -> "Tamil";
            case "4" -> "Telugu";
            default -> "English";
        };

        System.out.println("\n[Saathi Voice Simulation]: Hello! Saathi is ready to assist you in " + selectedLang + ".");
        System.out.println("Please provide your details below (Voice-to-Text simulation):\n");

        String name = promptNonEmpty(scanner, "Enter Name: ");
        String phone = promptNonEmpty(scanner, "Enter Phone Number: ");

        System.out.print("Enter Education Level (e.g., 10th Pass, 12th Pass, Graduate, ITI, None): ");
        String education = scanner.nextLine().trim();
        if (education.isEmpty()) education = "Not Specified";

        System.out.print("Enter Family/Traditional Occupation (e.g., Agriculture, Weaving, Leatherwork, Artisan): ");
        String familyOccupation = scanner.nextLine().trim();
        if (familyOccupation.isEmpty()) familyOccupation = "None";

        System.out.print("Enter Current Livelihood/Occupation (e.g., Daily Wager, Tailor Assistant, Unemployed): ");
        String currentLivelihood = scanner.nextLine().trim();
        if (currentLivelihood.isEmpty()) currentLivelihood = "Unemployed";

        System.out.print("Enter Existing Skills (comma-separated, e.g., Basic Computer, Sewing, Wiring): ");
        String skills = scanner.nextLine().trim();
        if (skills.isEmpty()) skills = "Basic Communication";

        System.out.print("Enter Interests (comma-separated, e.g., Electronics, Fashion, Data Entry, Business): ");
        String interests = scanner.nextLine().trim();
        if (interests.isEmpty()) interests = "General Employment";

        System.out.print("Enter Future Aspirations (e.g., Start Small Business, Computer Operator, Technician): ");
        String aspirations = scanner.nextLine().trim();
        if (aspirations.isEmpty()) aspirations = "Gainful Livelihood";

        System.out.print("Enter Mobility/Physical Constraints (e.g., Local only, Night shift restriction, None): ");
        String constraints = scanner.nextLine().trim();
        if (constraints.isEmpty()) constraints = "None";

        System.out.println("Select Employment Preference:");
        System.out.println("1. Wage Employment");
        System.out.println("2. Self Employment");
        System.out.println("3. Both");
        System.out.print("Choice (1-3): ");
        String prefChoice = scanner.nextLine().trim();
        String empPref = "Both";
        if (prefChoice.equals("1")) empPref = "Wage Employment";
        else if (prefChoice.equals("2")) empPref = "Self Employment";

        System.out.print("Enter Region / District (e.g., Rural - Salem, Urban - Delhi, Patna): ");
        String region = scanner.nextLine().trim();
        if (region.isEmpty()) region = "District Level";

        // Auto Generate Beneficiary ID
        String idStr = String.valueOf(nextBeneficiaryId++);
        String regDate = new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date());

        Beneficiary beneficiary = new Beneficiary(
                idStr, name, phone, selectedLang, education, familyOccupation,
                currentLivelihood, skills, interests, aspirations, constraints,
                empPref, region, "Profile Created", regDate
        );

        beneficiaries.add(beneficiary);
        saveBeneficiaryToDb(beneficiary);

        System.out.println("\n-------------------------------------------------");
        System.out.println("✓ Beneficiary registered successfully!");
        System.out.println("✓ Beneficiary ID: " + idStr);
        System.out.println("✓ Status: Profile Created");
        System.out.println("-------------------------------------------------\n");
    }

    private static String promptNonEmpty(Scanner scanner, String prompt) {
        String input = "";
        while (input.isEmpty()) {
            System.out.print(prompt);
            input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                System.out.println("[X] This field cannot be empty. Please enter a value.");
            }
        }
        return input;
    }

    // Helper method to find a beneficiary by ID
    public static Beneficiary findBeneficiaryById(String id) {
        if (id == null || id.trim().isEmpty()) return null;
        for (Beneficiary b : beneficiaries) {
            if (b.getId().equalsIgnoreCase(id.trim())) {
                return b;
            }
        }
        return null;
    }

    // ========================================================================
    // 2. VIEW BENEFICIARY PROFILE
    // ========================================================================
    private static void viewBeneficiaryProfile(Scanner scanner) {
        System.out.println("\n-------------------------------------------------");
        System.out.println("             VIEW BENEFICIARY PROFILE            ");
        System.out.println("-------------------------------------------------");
        System.out.print("Enter Beneficiary ID: ");
        String id = scanner.nextLine().trim();

        Beneficiary b = findBeneficiaryById(id);
        if (b == null) {
            System.out.println("[X] Beneficiary with ID '" + id + "' not found.\n");
            return;
        }

        printBeneficiaryProfileDetails(b);
    }

    private static void printBeneficiaryProfileDetails(Beneficiary b) {
        System.out.println("\n=================================================");
        System.out.println("BENEFICIARY PROFILE: " + b.getName() + " (ID: " + b.getId() + ")");
        System.out.println("=================================================");
        System.out.println("Phone Number           : " + b.getPhone());
        System.out.println("Preferred Language     : " + b.getLanguage());
        System.out.println("Education Level        : " + b.getEducation());
        System.out.println("Family Occupation      : " + b.getFamilyOccupation());
        System.out.println("Current Livelihood     : " + b.getCurrentLivelihood());
        System.out.println("Existing Skills        : " + b.getSkills());
        System.out.println("Interests              : " + b.getInterests());
        System.out.println("Future Aspirations     : " + b.getAspirations());
        System.out.println("Mobility Constraints   : " + b.getConstraints());
        System.out.println("Employment Preference  : " + b.getEmploymentPreference());
        System.out.println("Region / District      : " + b.getRegion());
        System.out.println("Pipeline Status        : " + b.getStatus());
        System.out.println("Registration Date      : " + b.getRegistrationDate());
        System.out.println("=================================================\n");
    }

    // ========================================================================
    // 3. GENERATE AI TRAINING RECOMMENDATIONS
    // ========================================================================
    private static void generateTrainingRecommendations(Scanner scanner) {
        System.out.println("\n-------------------------------------------------");
        System.out.println("     AI-POWERED NSQF TRAINING RECOMMENDATIONS   ");
        System.out.println("-------------------------------------------------");
        System.out.print("Enter Beneficiary ID: ");
        String id = scanner.nextLine().trim();

        Beneficiary b = findBeneficiaryById(id);
        if (b == null) {
            System.out.println("[X] Beneficiary with ID '" + id + "' not found.\n");
            return;
        }

        List<Recommendation<TrainingProgram>> recommendations = calculateTrainingRecommendations(b);

        System.out.println("\n=================================================");
        System.out.println("TOP TRAINING RECOMMENDATIONS FOR: " + b.getName());
        System.out.println("=================================================");

        int rank = 1;
        for (Recommendation<TrainingProgram> rec : recommendations) {
            TrainingProgram tp = rec.getItem();
            System.out.println("\n" + rank + ". " + tp.getProgramName() + " (NSQF Level " + tp.getNsqfLevel() + ")");
            System.out.println("   Match Score: " + rec.getScore() + "%");
            System.out.println("   Duration   : " + tp.getDuration() + " | Type: " + tp.getEmploymentType());
            System.out.println("   Description: " + tp.getDescription());
            System.out.println("   Why Recommended:");
            for (String reason : rec.getReasons()) {
                System.out.println("     • " + reason);
            }
            rank++;
        }
        System.out.println("=================================================\n");

        if (b.getStatus().equalsIgnoreCase("Profile Created")) {
            b.setStatus("Recommendation Generated");
            saveBeneficiaryToDb(b);
            System.out.println("[+] Beneficiary status updated to: 'Recommendation Generated'\n");
        }
    }

    public static List<Recommendation<TrainingProgram>> calculateTrainingRecommendations(Beneficiary b) {
        List<Recommendation<TrainingProgram>> list = new ArrayList<>();

        for (TrainingProgram tp : trainingPrograms) {
            int score = 0;
            List<String> reasons = new ArrayList<>();

            // 1. Skill Match (+25)
            if (matchesKeyword(b.getSkills(), tp.getSkillsKeywords())) {
                score += 25;
                reasons.add("Matches beneficiary's existing skill sets (" + tp.getSkillsKeywords() + ")");
            }

            // 2. Interest Match (+25)
            if (matchesKeyword(b.getInterests(), tp.getSkillsKeywords()) || matchesKeyword(b.getInterests(), tp.getProgramName())) {
                score += 25;
                reasons.add("Aligns with beneficiary's stated interests (" + b.getInterests() + ")");
            }

            // 3. Aspiration Match (+20)
            if (matchesKeyword(b.getAspirations(), tp.getProgramName()) || matchesKeyword(b.getAspirations(), tp.getDescription())) {
                score += 20;
                reasons.add("Fulfills long-term career aspirations (" + b.getAspirations() + ")");
            }

            // 4. Region Match (+15)
            if (tp.getRegion().equalsIgnoreCase("All") || matchesKeyword(b.getRegion(), tp.getRegion())) {
                score += 15;
                reasons.add("Available in or suitable for beneficiary's region (" + b.getRegion() + ")");
            }

            // 5. Employment Preference (+10)
            if (b.getEmploymentPreference().equalsIgnoreCase("Both") || b.getEmploymentPreference().equalsIgnoreCase(tp.getEmploymentType())) {
                score += 10;
                reasons.add("Matches employment preference (" + b.getEmploymentPreference() + ")");
            }

            // 6. Current/Family Occupation Match (+5)
            if (matchesKeyword(b.getFamilyOccupation(), tp.getDescription()) || matchesKeyword(b.getCurrentLivelihood(), tp.getDescription())) {
                score += 5;
                reasons.add("Builds upon traditional family occupation (" + b.getFamilyOccupation() + ")");
            }

            // Default minimum boost if low matching
            if (score == 0) {
                score = 30;
                reasons.add("General NSQF foundation skill program suitable for career growth.");
            }

            score = Math.min(100, score);
            list.add(new Recommendation<>(tp, score, reasons));
        }

        // Sort descending by score
        list.sort((r1, r2) -> Integer.compare(r2.getScore(), r1.getScore()));
        return list;
    }

    // Helper keyword matching algorithm
    private static boolean matchesKeyword(String source, String target) {
        if (source == null || target == null) return false;
        String[] sourceTokens = source.toLowerCase().split("[,\\s]+");
        String targetLower = target.toLowerCase();

        for (String token : sourceTokens) {
            if (token.length() > 2 && targetLower.contains(token)) {
                return true;
            }
        }
        return false;
    }

    // ========================================================================
    // 4. GENERATE JOB / LIVELIHOOD RECOMMENDATIONS
    // ========================================================================
    private static void generateJobRecommendations(Scanner scanner) {
        System.out.println("\n-------------------------------------------------");
        System.out.println("   AI LOCAL JOB / LIVELIHOOD RECOMMENDATIONS    ");
        System.out.println("-------------------------------------------------");
        System.out.print("Enter Beneficiary ID: ");
        String id = scanner.nextLine().trim();

        Beneficiary b = findBeneficiaryById(id);
        if (b == null) {
            System.out.println("[X] Beneficiary with ID '" + id + "' not found.\n");
            return;
        }

        List<Recommendation<Opportunity>> recommendations = calculateOpportunityRecommendations(b);

        System.out.println("\n=================================================");
        System.out.println("TOP LIVELIHOOD / JOB OPPORTUNITIES FOR: " + b.getName());
        System.out.println("=================================================");

        int rank = 1;
        for (Recommendation<Opportunity> rec : recommendations) {
            Opportunity opp = rec.getItem();
            System.out.println("\n" + rank + ". " + opp.getOpportunityName() + " (" + opp.getType() + ")");
            System.out.println("   Match Score: " + rec.getScore() + "%");
            System.out.println("   Region     : " + opp.getRegion() + " | Required Skill: " + opp.getRequiredSkill());
            System.out.println("   Description: " + opp.getDescription());
            System.out.println("   Why Recommended:");
            for (String reason : rec.getReasons()) {
                System.out.println("     • " + reason);
            }
            rank++;
        }
        System.out.println("=================================================\n");
    }

    public static List<Recommendation<Opportunity>> calculateOpportunityRecommendations(Beneficiary b) {
        List<Recommendation<Opportunity>> list = new ArrayList<>();

        for (Opportunity opp : opportunities) {
            int score = 0;
            List<String> reasons = new ArrayList<>();

            // 1. Required Skill Match (+25)
            if (matchesKeyword(b.getSkills(), opp.getRequiredSkill())) {
                score += 25;
                reasons.add("Directly matches beneficiary's existing skill (" + opp.getRequiredSkill() + ")");
            }

            // 2. Interest Match (+25)
            if (matchesKeyword(b.getInterests(), opp.getOpportunityName()) || matchesKeyword(b.getInterests(), opp.getDescription())) {
                score += 25;
                reasons.add("Matches beneficiary's interests (" + b.getInterests() + ")");
            }

            // 3. Aspiration Match (+20)
            if (matchesKeyword(b.getAspirations(), opp.getOpportunityName()) || matchesKeyword(b.getAspirations(), opp.getType())) {
                score += 20;
                reasons.add("Aligns with beneficiary's career aspirations");
            }

            // 4. Region Match (+15)
            if (opp.getRegion().equalsIgnoreCase("All") || matchesKeyword(b.getRegion(), opp.getRegion())) {
                score += 15;
                reasons.add("Locally available in beneficiary's region (" + b.getRegion() + ")");
            }

            // 5. Employment Preference (+10)
            if (b.getEmploymentPreference().equalsIgnoreCase("Both") || b.getEmploymentPreference().equalsIgnoreCase(opp.getType())) {
                score += 10;
                reasons.add("Fits employment preference (" + b.getEmploymentPreference() + ")");
            }

            // 6. Current/Family Occupation Match (+5)
            if (matchesKeyword(b.getFamilyOccupation(), opp.getDescription()) || matchesKeyword(b.getCurrentLivelihood(), opp.getDescription())) {
                score += 5;
                reasons.add("Capitalizes on traditional background (" + b.getFamilyOccupation() + ")");
            }

            if (score == 0) {
                score = 35;
                reasons.add("Entry-level local livelihood opportunity with on-the-job orientation.");
            }

            score = Math.min(100, score);
            list.add(new Recommendation<>(opp, score, reasons));
        }

        list.sort((r1, r2) -> Integer.compare(r2.getScore(), r1.getScore()));
        return list;
    }

    // ========================================================================
    // 5. VIEW PERSONALIZED LIVELIHOOD ROADMAP & SKILL GAP
    // ========================================================================
    private static void viewPersonalizedRoadmap(Scanner scanner) {
        System.out.println("\n-------------------------------------------------");
        System.out.println("         PERSONALIZED LIVELIHOOD ROADMAP         ");
        System.out.println("-------------------------------------------------");
        System.out.print("Enter Beneficiary ID: ");
        String id = scanner.nextLine().trim();

        Beneficiary b = findBeneficiaryById(id);
        if (b == null) {
            System.out.println("[X] Beneficiary with ID '" + id + "' not found.\n");
            return;
        }

        // Get top training & top opportunity recommendations
        List<Recommendation<TrainingProgram>> topTrainings = calculateTrainingRecommendations(b);
        List<Recommendation<Opportunity>> topOpportunities = calculateOpportunityRecommendations(b);

        TrainingProgram topTP = topTrainings.isEmpty() ? trainingPrograms.get(0) : topTrainings.get(0).getItem();
        Opportunity topOpp = topOpportunities.isEmpty() ? opportunities.get(0) : topOpportunities.get(0).getItem();

        // Perform Skill Gap Analysis
        List<String> missingSkills = performSkillGapAnalysis(b.getSkills(), topTP.getSkillsKeywords());

        System.out.println("\n====================================================================");
        System.out.println("         PERSONALIZED LIVELIHOOD ROADMAP FOR: " + b.getName().toUpperCase());
        System.out.println("====================================================================");
        System.out.println("Beneficiary ID: " + b.getId() + " | Current Status: " + b.getStatus());
        System.out.println("Region: " + b.getRegion() + " | Preference: " + b.getEmploymentPreference());
        System.out.println("--------------------------------------------------------------------");

        System.out.println("\n[STEP 1] CURRENT SKILLS PROFILE");
        System.out.println("         • Existing Skills: " + b.getSkills());
        System.out.println("         • Current Livelihood: " + b.getCurrentLivelihood());
        System.out.println("                 ↓");

        System.out.println("[STEP 2] SKILL GAP ANALYSIS");
        System.out.println("         • Target Programme: " + topTP.getProgramName());
        System.out.println("         • Required Skills: " + topTP.getSkillsKeywords());
        System.out.println("         • Skill Gaps Identified: " + (missingSkills.isEmpty() ? "None (Ready for direct placement)" : String.join(", ", missingSkills)));
        System.out.println("                 ↓");

        System.out.println("[STEP 3] RECOMMENDED NSQF TRAINING PROGRAMME");
        System.out.println("         • Programme: " + topTP.getProgramName() + " (NSQF Level " + topTP.getNsqfLevel() + ")");
        System.out.println("         • Duration : " + topTP.getDuration());
        System.out.println("         • Action   : Complete enrolment via PM-DAKSH / NBCFDC skill portal");
        System.out.println("                 ↓");

        System.out.println("[STEP 4] TRAINING COMPLETION & CERTIFICATION");
        System.out.println("         • Milestone: NSQF Skill Certification & Assessment");
        System.out.println("                 ↓");

        System.out.println("[STEP 5] TARGET LIVELIHOOD / JOB PLACEMENT");
        System.out.println("         • Target Role: " + topOpp.getOpportunityName() + " (" + topOpp.getType() + ")");
        System.out.println("         • Required Skill: " + topOpp.getRequiredSkill());
        System.out.println("                 ↓");

        System.out.println("[STEP 6] SUSTAINABLE NEXT STEPS");
        System.out.println("         • Linkage to regional employers or micro-credit financial schemes");
        System.out.println("         • Ongoing monitoring by District Welfare Officer");
        System.out.println("====================================================================\n");
    }

    public static List<String> performSkillGapAnalysis(String existingSkills, String requiredSkills) {
        List<String> gaps = new ArrayList<>();
        if (requiredSkills == null || requiredSkills.isEmpty()) return gaps;

        String[] reqArray = requiredSkills.split("[,/\\s]+");
        String existingLower = existingSkills.toLowerCase();

        for (String req : reqArray) {
            req = req.trim();
            if (req.length() > 2 && !existingLower.contains(req.toLowerCase())) {
                if (!gaps.contains(req)) {
                    gaps.add(req);
                }
            }
        }
        return gaps;
    }

    // ========================================================================
    // 6. UPDATE TRAINING / PLACEMENT STATUS
    // ========================================================================
    private static void updateTrainingPlacementStatus(Scanner scanner) {
        System.out.println("\n-------------------------------------------------");
        System.out.println("      UPDATE TRAINING & PLACEMENT STATUS         ");
        System.out.println("-------------------------------------------------");
        System.out.print("Enter Beneficiary ID: ");
        String id = scanner.nextLine().trim();

        Beneficiary b = findBeneficiaryById(id);
        if (b == null) {
            System.out.println("[X] Beneficiary with ID '" + id + "' not found.\n");
            return;
        }

        System.out.println("\nCurrent Status for " + b.getName() + ": " + b.getStatus());
        System.out.println("\nSelect New Pipeline Status:");
        System.out.println("1. Profile Created");
        System.out.println("2. Recommendation Generated");
        System.out.println("3. Enrolled");
        System.out.println("4. Training In Progress");
        System.out.println("5. Training Completed");
        System.out.println("6. Placed");
        System.out.println("7. Self-Employment Started");
        System.out.println("8. Needs Officer Support");
        System.out.print("Choice (1-8): ");
        String choice = scanner.nextLine().trim();

        String newStatus = switch (choice) {
            case "1" -> "Profile Created";
            case "2" -> "Recommendation Generated";
            case "3" -> "Enrolled";
            case "4" -> "Training In Progress";
            case "5" -> "Training Completed";
            case "6" -> "Placed";
            case "7" -> "Self-Employment Started";
            case "8" -> "Needs Officer Support";
            default -> {
                System.out.println("[X] Invalid status selection.\n");
                yield null;
            }
        };

        if (newStatus == null) {
            return;
        }

        b.setStatus(newStatus);
        saveBeneficiaryToDb(b);
        System.out.println("\n[✓] Status updated successfully to: '" + newStatus + "' for " + b.getName() + ".\n");

        if (newStatus.equalsIgnoreCase("Needs Officer Support")) {
            System.out.println("-------------------------------------------------");
            System.out.println("⚠️ HUMAN-IN-THE-LOOP ALERT TRIGGERED!");
            System.out.println("This beneficiary requires human assistance.");
            System.out.println("Please review profile & recommendations in Officer Dashboard.");
            System.out.println("-------------------------------------------------\n");
        }
    }

    // ========================================================================
    // 7. OFFICER DASHBOARD (Monitoring & Pipeline)
    // ========================================================================
    private static void officerDashboard(Scanner scanner) {
        System.out.println("\n=================================================");
        System.out.println("            DISTRICT OFFICER DASHBOARD           ");
        System.out.println("    Training-to-Placement Pipeline Monitoring    ");
        System.out.println("=================================================");

        int total = beneficiaries.size();
        int profilesCreated = 0;
        int recGenerated = 0;
        int enrolled = 0;
        int inProgress = 0;
        int completed = 0;
        int placed = 0;
        int selfEmployed = 0;
        int needsSupport = 0;

        for (Beneficiary b : beneficiaries) {
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

        System.out.println("SYSTEM METRICS:");
        System.out.println("• Total Beneficiaries Registered : " + total);
        System.out.println("• Profiles Created               : " + profilesCreated);
        System.out.println("• Recommendations Generated      : " + recGenerated);
        System.out.println("• Enrolled in Training           : " + enrolled);
        System.out.println("• Training In Progress           : " + inProgress);
        System.out.println("• Training Completed             : " + completed);
        System.out.println("• Placed in Wage Employment      : " + placed);
        System.out.println("• Self-Employment Started        : " + selfEmployed);
        System.out.println("• Requires Officer Support (HITL): " + needsSupport);
        System.out.println("-------------------------------------------------");

        System.out.println("\nBENEFICIARY PIPELINE OVERVIEW:");
        System.out.printf("%-6s | %-18s | %-15s | %-22s\n", "ID", "Name", "Region", "Status");
        System.out.println("-------------------------------------------------------------------");
        for (Beneficiary b : beneficiaries) {
            System.out.printf("%-6s | %-18s | %-15s | %-22s\n",
                    b.getId(), truncate(b.getName(), 18), truncate(b.getRegion(), 15), b.getStatus());
        }
        System.out.println("-------------------------------------------------------------------");

        if (needsSupport > 0) {
            System.out.println("\n⚠️ ATTENTION: There are " + needsSupport + " beneficiary(ies) marked 'Needs Officer Support'.");
            System.out.print("Would you like to review Human-in-the-Loop cases now? (y/n): ");
            String ans = scanner.nextLine().trim();
            if (ans.equalsIgnoreCase("y")) {
                for (Beneficiary b : beneficiaries) {
                    if (b.getStatus().equalsIgnoreCase("Needs Officer Support")) {
                        System.out.println("\n=================================================");
                        System.out.println("HUMAN-IN-THE-LOOP ASSISTANCE REVIEW: " + b.getName());
                        System.out.println("=================================================");
                        System.out.println("This beneficiary requires human assistance.");
                        System.out.println("Please review the profile and recommendation before proceeding.");
                        printBeneficiaryProfileDetails(b);
                    }
                }
            }
        }
        System.out.println();
    }

    private static String truncate(String text, int width) {
        if (text == null) return "";
        if (text.length() <= width) return text;
        return text.substring(0, width - 2) + "..";
    }

    // ========================================================================
    // 8. VIEW TRAINING PROGRAMMES (NSQF Aligned)
    // ========================================================================
    private static void viewTrainingPrograms() {
        System.out.println("\n=================================================");
        System.out.println("   AVAILABLE NSQF-ALIGNED TRAINING PROGRAMMES   ");
        System.out.println("=================================================");
        for (TrainingProgram tp : trainingPrograms) {
            System.out.println("ID        : " + tp.getTrainingId());
            System.out.println("Programme : " + tp.getProgramName() + " (NSQF Level " + tp.getNsqfLevel() + ")");
            System.out.println("Region    : " + tp.getRegion() + " | Type: " + tp.getEmploymentType());
            System.out.println("Duration  : " + tp.getDuration());
            System.out.println("Keywords  : " + tp.getSkillsKeywords());
            System.out.println("Description: " + tp.getDescription());
            System.out.println("-------------------------------------------------");
        }
        System.out.println();
    }

    // ========================================================================
    // 9. VIEW LOCAL OPPORTUNITIES
    // ========================================================================
    private static void viewLocalOpportunities() {
        System.out.println("\n=================================================");
        System.out.println("      LOCAL JOB & LIVELIHOOD OPPORTUNITIES       ");
        System.out.println("=================================================");
        for (Opportunity opp : opportunities) {
            System.out.println("ID         : " + opp.getOpportunityId());
            System.out.println("Opportunity: " + opp.getOpportunityName() + " (" + opp.getType() + ")");
            System.out.println("Region     : " + opp.getRegion());
            System.out.println("Req Skill  : " + opp.getRequiredSkill());
            System.out.println("Description: " + opp.getDescription());
            System.out.println("-------------------------------------------------");
        }
        System.out.println();
    }

    // ========================================================================
    // 10. SEARCH BENEFICIARY
    // ========================================================================
    private static void searchBeneficiary(Scanner scanner) {
        System.out.println("\n-------------------------------------------------");
        System.out.println("               SEARCH BENEFICIARY                ");
        System.out.println("-------------------------------------------------");
        System.out.print("Enter Search Term (ID / Name / Phone / Region): ");
        String query = scanner.nextLine().trim().toLowerCase();

        if (query.isEmpty()) {
            System.out.println("[X] Search query cannot be empty.\n");
            return;
        }

        List<Beneficiary> results = new ArrayList<>();
        for (Beneficiary b : beneficiaries) {
            if (b.getId().toLowerCase().contains(query) ||
                b.getName().toLowerCase().contains(query) ||
                b.getPhone().toLowerCase().contains(query) ||
                b.getRegion().toLowerCase().contains(query)) {
                results.add(b);
            }
        }

        if (results.isEmpty()) {
            System.out.println("[!] No beneficiaries matching '" + query + "' were found.\n");
        } else {
            System.out.println("\n[✓] Found " + results.size() + " matching beneficiary(ies):");
            for (Beneficiary b : results) {
                printBeneficiaryProfileDetails(b);
            }
        }
    }

    // ========================================================================
    // 11 & 12. DATA PERSISTENCE (SQLITE SAATHI.DB)
    // ========================================================================
    public static void saveBeneficiaryToDb(Beneficiary b) {
        String sql = """
            INSERT INTO beneficiaries (
                id, name, phone, language, education, family_occupation,
                current_livelihood, skills, interests, aspirations, constraints,
                employment_preference, region, status, registration_date
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(id) DO UPDATE SET
                name=excluded.name,
                phone=excluded.phone,
                language=excluded.language,
                education=excluded.education,
                family_occupation=excluded.family_occupation,
                current_livelihood=excluded.current_livelihood,
                skills=excluded.skills,
                interests=excluded.interests,
                aspirations=excluded.aspirations,
                constraints=excluded.constraints,
                employment_preference=excluded.employment_preference,
                region=excluded.region,
                status=excluded.status,
                registration_date=excluded.registration_date;
        """;
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, b.getId());
            pstmt.setString(2, b.getName());
            pstmt.setString(3, b.getPhone());
            pstmt.setString(4, b.getLanguage());
            pstmt.setString(5, b.getEducation());
            pstmt.setString(6, b.getFamilyOccupation());
            pstmt.setString(7, b.getCurrentLivelihood());
            pstmt.setString(8, b.getSkills());
            pstmt.setString(9, b.getInterests());
            pstmt.setString(10, b.getAspirations());
            pstmt.setString(11, b.getConstraints());
            pstmt.setString(12, b.getEmploymentPreference());
            pstmt.setString(13, b.getRegion());
            pstmt.setString(14, b.getStatus());
            pstmt.setString(15, b.getRegistrationDate());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[X] Error saving beneficiary to SQLite 'saathi.db': " + e.getMessage());
        }
    }

    private static void saveTrainingProgramToDb(TrainingProgram tp) {
        String sql = """
            INSERT INTO training_programs (
                training_id, program_name, nsqf_level, region, employment_type,
                skills_keywords, duration, description
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(training_id) DO UPDATE SET
                program_name=excluded.program_name,
                nsqf_level=excluded.nsqf_level,
                region=excluded.region,
                employment_type=excluded.employment_type,
                skills_keywords=excluded.skills_keywords,
                duration=excluded.duration,
                description=excluded.description;
        """;
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, tp.getTrainingId());
            pstmt.setString(2, tp.getProgramName());
            pstmt.setInt(3, tp.getNsqfLevel());
            pstmt.setString(4, tp.getRegion());
            pstmt.setString(5, tp.getEmploymentType());
            pstmt.setString(6, tp.getSkillsKeywords());
            pstmt.setString(7, tp.getDuration());
            pstmt.setString(8, tp.getDescription());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[X] Error saving training program to SQLite: " + e.getMessage());
        }
    }

    private static void saveOpportunityToDb(Opportunity opp) {
        String sql = """
            INSERT INTO opportunities (
                opportunity_id, opportunity_name, region, type, required_skill, description
            ) VALUES (?, ?, ?, ?, ?, ?)
            ON CONFLICT(opportunity_id) DO UPDATE SET
                opportunity_name=excluded.opportunity_name,
                region=excluded.region,
                type=excluded.type,
                required_skill=excluded.required_skill,
                description=excluded.description;
        """;
        try (Connection conn = getConnection(); PreparedStatement pstmt = conn.prepareStatement(sql)) {
            pstmt.setString(1, opp.getOpportunityId());
            pstmt.setString(2, opp.getOpportunityName());
            pstmt.setString(3, opp.getRegion());
            pstmt.setString(4, opp.getType());
            pstmt.setString(5, opp.getRequiredSkill());
            pstmt.setString(6, opp.getDescription());
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[X] Error saving opportunity to SQLite: " + e.getMessage());
        }
    }

    private static void saveData() {
        int count = 0;
        for (Beneficiary b : beneficiaries) {
            saveBeneficiaryToDb(b);
            count++;
        }
        System.out.println("[✓] " + count + " beneficiary record(s) persisted to 'saathi.db'.");
    }

    public static void loadData() {
        beneficiaries.clear();
        String sql = "SELECT * FROM beneficiaries ORDER BY CAST(id AS INTEGER) ASC";
        int maxId = 1000;
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Beneficiary b = new Beneficiary(
                        rs.getString("id"),
                        rs.getString("name"),
                        rs.getString("phone"),
                        rs.getString("language"),
                        rs.getString("education"),
                        rs.getString("family_occupation"),
                        rs.getString("current_livelihood"),
                        rs.getString("skills"),
                        rs.getString("interests"),
                        rs.getString("aspirations"),
                        rs.getString("constraints"),
                        rs.getString("employment_preference"),
                        rs.getString("region"),
                        rs.getString("status"),
                        rs.getString("registration_date")
                );
                beneficiaries.add(b);
                try {
                    int idNum = Integer.parseInt(b.getId());
                    if (idNum > maxId) maxId = idNum;
                } catch (NumberFormatException ignored) {}
            }

            if (beneficiaries.isEmpty()) {
                migrateLegacyTextData();
                return;
            }

            nextBeneficiaryId = maxId + 1;
            System.out.println("[✓] Loaded " + beneficiaries.size() + " beneficiary record(s) from 'saathi.db'.");
        } catch (SQLException e) {
            System.out.println("[X] Warning: Could not load saved data from SQLite: " + e.getMessage());
        }
    }

    private static void migrateLegacyTextData() {
        File file = new File("saathi_beneficiaries.txt");
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int maxId = 1000;
            int count = 0;
            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) continue;
                Beneficiary b = Beneficiary.fromDataString(line);
                if (b != null) {
                    beneficiaries.add(b);
                    saveBeneficiaryToDb(b);
                    count++;
                    try {
                        int idNum = Integer.parseInt(b.getId());
                        if (idNum > maxId) maxId = idNum;
                    } catch (NumberFormatException ignored) {}
                }
            }
            nextBeneficiaryId = maxId + 1;
            if (count > 0) {
                System.out.println("[✓] Migrated " + count + " legacy record(s) from 'saathi_beneficiaries.txt' to 'saathi.db'.");
            }
        } catch (IOException e) {
            System.out.println("[X] Warning: Could not migrate legacy text data: " + e.getMessage());
        }
    }

    // ========================================================================
    // SAMPLE DATA INITIALIZATION
    // ========================================================================
    public static void loadSampleData() {
        trainingPrograms.clear();
        opportunities.clear();

        // 1. Seed Training Programs if DB table is empty
        List<TrainingProgram> samplePrograms = Arrays.asList(
                new TrainingProgram("TP101", "Digital Skills & Data Entry", 4, "All", "Wage Employment",
                        "Basic Computer, Typing, Data Entry, Spreadsheet, MS Office", "3 Months",
                        "Foundation course in IT skills, spreadsheet accounting, and digital office management."),
                new TrainingProgram("TP102", "Tailoring & Garment Entrepreneurship", 3, "Rural", "Self Employment",
                        "Sewing, Stitching, Tailoring, Garment Design, Business", "4 Months",
                        "Hands-on garment construction, apparel design, and micro-enterprise management."),
                new TrainingProgram("TP103", "Electrical Technician & Solar Maintenance", 4, "Urban", "Wage Employment",
                        "Wiring, Electrical, Solar Panel, Appliance Repair, Maintenance", "6 Months",
                        "Domestic and commercial electrical installations, safety standards, and solar maintenance."),
                new TrainingProgram("TP104", "Food Processing & Small Enterprise", 3, "Rural", "Self Employment",
                        "Food Processing, Packaging, Food Safety, Business, Preserves", "3 Months",
                        "Processing local produce into packaged jams, pickles, snacks with FSSAI compliance."),
                new TrainingProgram("TP105", "Computer Hardware Technician", 4, "Urban", "Wage Employment",
                        "Hardware, Repair, Networking, Troubleshooting, PC Assembly", "6 Months",
                        "Computer assembly, motherboard repair, networking, and technical support."),
                new TrainingProgram("TP106", "Beauty and Wellness Entrepreneurship", 3, "All", "Both",
                        "Skincare, Hair Styling, Salon Management, Beauty", "3 Months",
                        "Professional cosmetology, skincare techniques, and home-salon enterprise training."),
                new TrainingProgram("TP107", "Organic Farming & Agritech Management", 4, "Rural", "Both",
                        "Farming, Organic Agriculture, Crop Management, Soil Testing", "4 Months",
                        "Modern organic farming methods, vermicomposting, and direct market linkages."),
                new TrainingProgram("TP108", "Handicraft & Artisan Skills", 3, "Rural", "Self Employment",
                        "Handicrafts, Weaving, Pottery, Artisan Work, Wood Carving", "3 Months",
                        "Traditional craft refinement, e-commerce marketing, and handicraft cluster production.")
        );

        for (TrainingProgram tp : samplePrograms) {
            saveTrainingProgramToDb(tp);
        }

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM training_programs")) {
            while (rs.next()) {
                trainingPrograms.add(new TrainingProgram(
                        rs.getString("training_id"),
                        rs.getString("program_name"),
                        rs.getInt("nsqf_level"),
                        rs.getString("region"),
                        rs.getString("employment_type"),
                        rs.getString("skills_keywords"),
                        rs.getString("duration"),
                        rs.getString("description")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[X] Warning: Could not load training programs from SQLite DB: " + e.getMessage());
        }

        // 2. Seed Opportunities if DB table is empty
        List<Opportunity> sampleOpps = Arrays.asList(
                new Opportunity("OP201", "Data Entry Operator", "Urban", "Wage Employment",
                        "Data Entry", "District e-Governance center data digitization and documentation assistant."),
                new Opportunity("OP202", "Tailoring Micro-Enterprise", "Rural", "Self Employment",
                        "Tailoring", "Establish home tailoring boutique with Mudra / PM-DAKSH subsidy scheme."),
                new Opportunity("OP203", "Electrical Service Assistant", "Urban", "Wage Employment",
                        "Wiring", "Assist licensed electrical contractors in residential wiring and appliance repair."),
                new Opportunity("OP204", "Food Processing SHG Unit", "Rural", "Self Employment",
                        "Food Processing", "Self-Help Group food unit for regional snacks and packaged spice manufacturing."),
                new Opportunity("OP205", "IT Support Assistant", "Urban", "Wage Employment",
                        "Hardware", "Desktop hardware support at municipal offices and CSC centers."),
                new Opportunity("OP206", "Local Artisan Cooperative", "Rural", "Self Employment",
                        "Handicrafts", "Produce handicraft artifacts for state handicraft export promotion councils."),
                new Opportunity("OP207", "Organic Agri-Produce Producer", "Rural", "Both",
                        "Farming", "Cultivate certified organic vegetables with local Farmer Producer Company (FPO).")
        );

        for (Opportunity opp : sampleOpps) {
            saveOpportunityToDb(opp);
        }

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM opportunities")) {
            while (rs.next()) {
                opportunities.add(new Opportunity(
                        rs.getString("opportunity_id"),
                        rs.getString("opportunity_name"),
                        rs.getString("region"),
                        rs.getString("type"),
                        rs.getString("required_skill"),
                        rs.getString("description")
                ));
            }
        } catch (SQLException e) {
            System.err.println("[X] Warning: Could not load opportunities from SQLite DB: " + e.getMessage());
        }
    }

    // ========================================================================
    // ONBOARDING SESSION PERSISTENCE
    // ========================================================================

    public static void saveOnboardingSessionToDb(BeneficiaryOnboardingSession s) {
        String sql = """
            INSERT INTO onboarding_sessions (
                session_id, mode, language, current_step, collected_json,
                status, beneficiary_id, created_at, updated_at
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            ON CONFLICT(session_id) DO UPDATE SET
                mode=excluded.mode,
                language=excluded.language,
                current_step=excluded.current_step,
                collected_json=excluded.collected_json,
                status=excluded.status,
                beneficiary_id=excluded.beneficiary_id,
                updated_at=excluded.updated_at;
        """;
        try (Connection conn = getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, s.sessionId);
            ps.setString(2, s.mode);
            ps.setString(3, s.language);
            ps.setInt(4, s.currentStep);
            ps.setString(5, s.collectedDataToJson());
            ps.setString(6, s.status);
            ps.setString(7, s.beneficiaryId);
            ps.setString(8, s.createdAt);
            ps.setString(9, new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new java.util.Date()));
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("[X] Error saving onboarding session: " + e.getMessage());
        }
    }

    // ========================================================================
    // NESTED CLASSES (OOP MODELS)
    // ========================================================================

    /**
     * Beneficiary Data Model
     */
    public static class Beneficiary {
        private final String id;
        private final String name;
        private final String phone;
        private final String language;
        private final String education;
        private final String familyOccupation;
        private final String currentLivelihood;
        private final String skills;
        private final String interests;
        private final String aspirations;
        private final String constraints;
        private final String employmentPreference;
        private final String region;
        private String status;
        private final String registrationDate;

        public Beneficiary(String id, String name, String phone, String language, String education,
                           String familyOccupation, String currentLivelihood, String skills,
                           String interests, String aspirations, String constraints,
                           String employmentPreference, String region, String status, String registrationDate) {
            this.id = id;
            this.name = name;
            this.phone = phone;
            this.language = language;
            this.education = education;
            this.familyOccupation = familyOccupation;
            this.currentLivelihood = currentLivelihood;
            this.skills = skills;
            this.interests = interests;
            this.aspirations = aspirations;
            this.constraints = constraints;
            this.employmentPreference = employmentPreference;
            this.region = region;
            this.status = status;
            this.registrationDate = registrationDate;
        }

        public String getId() { return id; }
        public String getName() { return name; }
        public String getPhone() { return phone; }
        public String getLanguage() { return language; }
        public String getEducation() { return education; }
        public String getFamilyOccupation() { return familyOccupation; }
        public String getCurrentLivelihood() { return currentLivelihood; }
        public String getSkills() { return skills; }
        public String getInterests() { return interests; }
        public String getAspirations() { return aspirations; }
        public String getConstraints() { return constraints; }
        public String getEmploymentPreference() { return employmentPreference; }
        public String getRegion() { return region; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        public String getRegistrationDate() { return registrationDate; }

        public String toDataString() {
            return id + "|" + escape(name) + "|" + escape(phone) + "|" + escape(language) + "|" +
                   escape(education) + "|" + escape(familyOccupation) + "|" + escape(currentLivelihood) + "|" +
                   escape(skills) + "|" + escape(interests) + "|" + escape(aspirations) + "|" +
                   escape(constraints) + "|" + escape(employmentPreference) + "|" + escape(region) + "|" +
                   escape(status) + "|" + escape(registrationDate);
        }

        public static Beneficiary fromDataString(String line) {
            String[] parts = line.split("\\|", -1);
            if (parts.length < 15) return null;
            return new Beneficiary(
                    parts[0], unescape(parts[1]), unescape(parts[2]), unescape(parts[3]),
                    unescape(parts[4]), unescape(parts[5]), unescape(parts[6]), unescape(parts[7]),
                    unescape(parts[8]), unescape(parts[9]), unescape(parts[10]), unescape(parts[11]),
                    unescape(parts[12]), unescape(parts[13]), unescape(parts[14])
            );
        }

        private static String escape(String str) {
            if (str == null) return "";
            return str.replace("|", ";");
        }

        private static String unescape(String str) {
            if (str == null) return "";
            return str.replace(";", "|");
        }
    }

    /**
     * Training Program Data Model
     */
    public static class TrainingProgram {
        private final String trainingId;
        private final String programName;
        private final int nsqfLevel;
        private final String region;
        private final String employmentType;
        private final String skillsKeywords;
        private final String duration;
        private final String description;

        public TrainingProgram(String trainingId, String programName, int nsqfLevel, String region,
                               String employmentType, String skillsKeywords, String duration, String description) {
            this.trainingId = trainingId;
            this.programName = programName;
            this.nsqfLevel = nsqfLevel;
            this.region = region;
            this.employmentType = employmentType;
            this.skillsKeywords = skillsKeywords;
            this.duration = duration;
            this.description = description;
        }

        public String getTrainingId() { return trainingId; }
        public String getProgramName() { return programName; }
        public int getNsqfLevel() { return nsqfLevel; }
        public String getRegion() { return region; }
        public String getEmploymentType() { return employmentType; }
        public String getSkillsKeywords() { return skillsKeywords; }
        public String getDuration() { return duration; }
        public String getDescription() { return description; }
    }

    /**
     * Opportunity Data Model
     */
    public static class Opportunity {
        private final String opportunityId;
        private final String opportunityName;
        private final String region;
        private final String type;
        private final String requiredSkill;
        private final String description;

        public Opportunity(String opportunityId, String opportunityName, String region,
                           String type, String requiredSkill, String description) {
            this.opportunityId = opportunityId;
            this.opportunityName = opportunityName;
            this.region = region;
            this.type = type;
            this.requiredSkill = requiredSkill;
            this.description = description;
        }

        public String getOpportunityId() { return opportunityId; }
        public String getOpportunityName() { return opportunityName; }
        public String getRegion() { return region; }
        public String getType() { return type; }
        public String getRequiredSkill() { return requiredSkill; }
        public String getDescription() { return description; }
    }

    /**
     * Recommendation Helper Wrapper Model
     */
    public static class Recommendation<T> {
        private final T item;
        private final int score;
        private final List<String> reasons;

        public Recommendation(T item, int score, List<String> reasons) {
            this.item = item;
            this.score = score;
            this.reasons = reasons;
        }

        public T getItem() { return item; }
        public int getScore() { return score; }
        public List<String> getReasons() { return reasons; }
    }

    // ========================================================================
    // BENEFICIARY ONBOARDING SESSION STATE MACHINE
    // ========================================================================

    /**
     * Holds the full conversational state for one onboarding session.
     * Fields map 1-to-1 with the 10 registration questions.
     */
    public static class BeneficiaryOnboardingSession {
        public String sessionId;
        public String mode;       // "direct" | "voice" | "ivr"
        public String language;   // BCP-47: "en-IN", "hi-IN", "ta-IN", "te-IN", "kn-IN"
        public int currentStep;   // 0 = not started, 1–10 = questions, 11 = complete
        public Map<String, String> collectedData = new LinkedHashMap<>();
        public String status;     // "active" | "completed" | "dropped"
        public String beneficiaryId = "";
        public String createdAt;

        // Ordered field keys matching Step 1–10
        public static final String[] FIELDS = {
            "name", "phone", "education", "familyOccupation", "currentLivelihood",
            "skills", "interests", "aspirations", "constraints", "employmentPreference", "region"
        };

        public BeneficiaryOnboardingSession(String sessionId, String mode, String language) {
            this.sessionId = sessionId;
            this.mode = mode;
            this.language = language;
            this.currentStep = 1;
            this.status = "active";
            this.createdAt = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss").format(new java.util.Date());
        }

        /** Returns the field key for the current step (steps 1-10 map to indices 0-9). */
        public String currentFieldKey() {
            if (currentStep < 1 || currentStep > 10) return null;
            return FIELDS[currentStep - 1];
        }

        /** Store a value for the current step field and advance. */
        public void submitAndAdvance(String value) {
            String key = currentFieldKey();
            if (key != null) collectedData.put(key, value);
            currentStep++;
        }

        public boolean isComplete() { return currentStep > 10; }

        /** Serialize collectedData to a simple JSON string for DB storage. */
        public String collectedDataToJson() {
            StringBuilder sb = new StringBuilder("{");
            boolean first = true;
            for (Map.Entry<String, String> e : collectedData.entrySet()) {
                if (!first) sb.append(",");
                sb.append("\"").append(e.getKey()).append("\": \"").append(e.getValue().replace("\\", "\\\\").replace("\"", "\\\"")).append("\"");
                first = false;
            }
            sb.append("}");
            return sb.toString();
        }
    }

    // ========================================================================
    // LOCALIZED PROMPTS (5 LANGUAGES × 11 STRINGS)
    // ========================================================================

    /**
     * Returns the localized prompt string for a given step in a given language.
     * Index 0 = welcome message; indices 1–10 = registration questions.
     */
    public static class LocalizedPrompts {
        // Map: languageCode → String[11] (index 0: welcome, 1–10: field questions)
        private static final Map<String, String[]> PROMPTS = new LinkedHashMap<>();

        static {
            PROMPTS.put("en-IN", new String[]{
                "Welcome to Saathi! I will guide you through 10 quick questions to set up your livelihood profile. Let's begin.",
                "Step 1: Please tell me your full name.",
                "Step 2: What is your 10-digit mobile phone number?",
                "Step 3: What is your highest education level? (For example: 10th Pass, 12th Pass, Graduate, ITI, or None)",
                "Step 4: What is your family or traditional occupation? (For example: Agriculture, Weaving, Leatherwork, Artisan)",
                "Step 5: What is your current livelihood or work? (For example: Unemployed, Daily Wage Worker, Tailor)",
                "Step 6: What are your existing skills or hobbies? (For example: Sewing, Basic Computer, Driving, Cooking)",
                "Step 7: What are your future career aspirations or interests?",
                "Step 8: Do you have any mobility or working constraints? (For example: Local area only, Day shifts only, None)",
                "Step 9: What type of employment do you prefer? Wage Employment, Self-Employment, or Both?",
                "Step 10: What is your region or district? (For example: Rural – Salem, Urban – Delhi)"
            });

            PROMPTS.put("hi-IN", new String[]{
                "साथी में आपका स्वागत है! मैं 10 सवालों के माध्यम से आपकी आजीविका प्रोफ़ाइल बनाऊँगा। चलिए शुरू करते हैं।",
                "चरण 1: कृपया अपना पूरा नाम बताएं।",
                "चरण 2: आपका 10 अंकों का मोबाइल नंबर क्या है?",
                "चरण 3: आपकी उच्चतम शिक्षा क्या है? (उदाहरण: 10वीं पास, 12वीं, ग्रेजुएट, आईटीआई, कोई नहीं)",
                "चरण 4: आपके परिवार का पारंपरिक व्यवसाय क्या है? (उदाहरण: कृषि, बुनाई, चमड़ा कार्य, कारीगरी)",
                "चरण 5: आप अभी क्या काम करते हैं? (उदाहरण: बेरोजगार, दैनिक मजदूर, दर्जी)",
                "चरण 6: आपके पास कौन से कौशल या शौक हैं? (उदाहरण: सिलाई, कंप्यूटर, ड्राइविंग)",
                "चरण 7: भविष्य में आप क्या करना चाहते हैं?",
                "चरण 8: क्या आपकी कोई आवागमन या काम की बाधा है? (उदाहरण: केवल स्थानीय, दिन की पाली, कोई नहीं)",
                "चरण 9: आप किस प्रकार का रोजगार चाहते हैं? वेतन रोजगार, स्वरोजगार, या दोनों?",
                "चरण 10: आपका जिला या क्षेत्र कौन सा है? (उदाहरण: ग्रामीण – वाराणसी, शहरी – दिल्ली)"
            });

            PROMPTS.put("ta-IN", new String[]{
                "சாதிக்கு வருக! 10 கேள்விகள் மூலம் உங்கள் வாழ்வாதார சுயவிவரத்தை உருவாக்குகிறேன். தொடங்கலாம்.",
                "படி 1: உங்கள் முழு பெயரை சொல்லுங்கள்.",
                "படி 2: உங்கள் 10 இலக்க மொபைல் எண் என்ன?",
                "படி 3: உங்கள் கல்வித் தகுதி என்ன? (உதாரணம்: 10ஆம் வகுப்பு, 12ஆம் வகுப்பு, பட்டதாரி, ITI, இல்லை)",
                "படி 4: உங்கள் குடும்பத்தின் பாரம்பரிய தொழில் என்ன? (உதாரணம்: விவசாயம், நெசவு, தோல் வேலை)",
                "படி 5: தற்போது நீங்கள் என்ன வேலை செய்கிறீர்கள்? (உதாரணம்: வேலையற்றவர், கூலி வேலை, தையற்காரர்)",
                "படி 6: உங்களுக்கு என்ன திறமைகள் அல்லது பொழுதுபோக்குகள் உள்ளன?",
                "படி 7: எதிர்காலத்தில் நீங்கள் என்ன செய்ய விரும்புகிறீர்கள்?",
                "படி 8: உங்களுக்கு ஏதேனும் பயண அல்லது வேலை கட்டுப்பாடுகள் உள்ளதா?",
                "படி 9: நீங்கள் எந்த வகையான வேலையை விரும்புகிறீர்கள்? ஊதிய வேலை, சுய வேலை, அல்லது இரண்டும்?",
                "படி 10: உங்கள் மாவட்டம் அல்லது பகுதி எது? (உதாரணம்: கிராமம் – சேலம், நகரம் – சென்னை)"
            });

            PROMPTS.put("te-IN", new String[]{
                "సాధి కి స్వాగతం! 10 ప్రశ్నల ద్వారా మీ జీవనోపాధి ప్రొఫైల్ తయారు చేస్తాను. మొదలుపెట్టండి.",
                "దశ 1: దయచేసి మీ పూర్తి పేరు చెప్పండి.",
                "దశ 2: మీ 10 అంకెల మొబైల్ నంబర్ ఏమిటి?",
                "దశ 3: మీ అత్యధిక విద్యా అర్హత ఏమిటి? (ఉదా: 10వ తరగతి, 12వ తరగతి, గ్రాడ్యుయేట్, ITI, లేదు)",
                "దశ 4: మీ కుటుంబ సాంప్రదాయ వృత్తి ఏమిటి? (ఉదా: వ్యవసాయం, నేత, చర్మ పని)",
                "దశ 5: మీరు ప్రస్తుతం ఏమి చేస్తున్నారు? (ఉదా: నిరుద్యోగి, రోజువారీ కూలీ, టైలర్)",
                "దశ 6: మీకు ఏ నైపుణ్యాలు లేదా హాబీలు ఉన్నాయి?",
                "దశ 7: భవిష్యత్తులో మీరు ఏమి చేయాలనుకుంటున్నారు?",
                "దశ 8: మీకు ఏదైనా పయన లేదా పని పరిమితులు ఉన్నాయా?",
                "దశ 9: మీరు ఏ రకమైన ఉద్యోగాన్ని ఇష్టపడతారు? వేతన ఉద్యోగం, స్వయం ఉపాధి, లేదా రెండూ?",
                "దశ 10: మీ జిల్లా లేదా ప్రాంతం ఏమిటి? (ఉదా: గ్రామీణ – తిరుపతి, పట్టణ – హైదరాబాద్)"
            });

            PROMPTS.put("kn-IN", new String[]{
                "ಸಾಥಿಗೆ ಸ್ವಾಗತ! 10 ಪ್ರಶ್ನೆಗಳ ಮೂಲಕ ನಿಮ್ಮ ಜೀವನೋಪಾಯ ಪ್ರೊಫೈಲ್ ರಚಿಸುತ್ತೇನೆ. ಪ್ರಾರಂಭಿಸೋಣ.",
                "ಹಂತ 1: ದಯವಿಟ್ಟು ನಿಮ್ಮ ಪೂರ್ಣ ಹೆಸರು ಹೇಳಿ.",
                "ಹಂತ 2: ನಿಮ್ಮ 10 ಅಂಕಿಯ ಮೊಬೈಲ್ ಸಂಖ್ಯೆ ಏನು?",
                "ಹಂತ 3: ನಿಮ್ಮ ಗರಿಷ್ಠ ಶಿಕ್ಷಣ ಮಟ್ಟ ಏನು? (ಉದಾ: 10ನೇ ತರಗತಿ, 12ನೇ ತರಗತಿ, ಪದವಿ, ITI, ಇಲ್ಲ)",
                "ಹಂತ 4: ನಿಮ್ಮ ಕುಟುಂಬದ ಸಾಂಪ್ರದಾಯಿಕ ವೃತ್ತಿ ಏನು? (ಉದಾ: ಕೃಷಿ, ನೇಕಾರಿಕೆ, ಚರ್ಮ ಕೆಲಸ)",
                "ಹಂತ 5: ನೀವು ಪ್ರಸ್ತುತ ಏನು ಮಾಡುತ್ತಿದ್ದೀರಿ? (ಉದಾ: ನಿರುದ್ಯೋಗಿ, ದಿನಗೂಲಿ, ದರ್ಜಿ)",
                "ಹಂತ 6: ನಿಮ್ಮ ಬಳಿ ಯಾವ ಕೌಶಲ್ಯ ಅಥವಾ ಹವ್ಯಾಸಗಳಿವೆ?",
                "ಹಂತ 7: ಭವಿಷ್ಯದಲ್ಲಿ ನೀವು ಏನು ಮಾಡಲು ಬಯಸುತ್ತೀರಿ?",
                "ಹಂತ 8: ನಿಮಗೆ ಯಾವುದಾದರೂ ಚಲನ ಅಥವಾ ಕೆಲಸದ ನಿರ್ಬಂಧಗಳಿವೆಯೇ?",
                "ಹಂತ 9: ನೀವು ಯಾವ ರೀತಿಯ ಉದ್ಯೋಗ ಬಯಸುತ್ತೀರಿ? ವೇತನ ಉದ್ಯೋಗ, ಸ್ವ-ಉದ್ಯೋಗ, ಅಥವಾ ಎರಡೂ?",
                "ಹಂತ 10: ನಿಮ್ಮ ಜಿಲ್ಲೆ ಅಥವಾ ಪ್ರದೇಶ ಯಾವುದು? (ಉದಾ: ಗ್ರಾಮೀಣ – ಮೈಸೂರು, ನಗರ – ಬೆಂಗಳೂರು)"
            });
        }

        public static String getWelcome(String lang) {
            return getPrompts(lang)[0];
        }

        public static String getPrompt(String lang, int step) {
            String[] arr = getPrompts(lang);
            if (step < 1 || step >= arr.length) return arr[arr.length - 1];
            return arr[step];
        }

        private static String[] getPrompts(String lang) {
            return PROMPTS.getOrDefault(lang, PROMPTS.get("en-IN"));
        }
    }

    // ========================================================================
    // CONVERSATIONAL EXTRACTION SERVICE (Slot-filling / Normalisation)
    // ========================================================================

    /**
     * Extracts and normalises the user's spoken/typed answer for each registration step.
     * Returns null if the answer fails validation (e.g. invalid phone number).
     */
    public static class ConversationalExtractionService {

        /**
         * @param step  1–10 matching the registration question index
         * @param raw   raw text from the user
         * @return      normalised value string, or null if invalid
         */
        public static String extract(int step, String raw) {
            if (raw == null) return null;
            String trimmed = raw.trim();
            if (trimmed.isEmpty()) return null;

            return switch (step) {
                case 1 -> extractName(trimmed);              // Full Name
                case 2 -> extractPhone(trimmed);             // Phone Number
                case 3 -> extractEducation(trimmed);         // Education Level
                case 4 -> trimmed;                           // Family Occupation (free text)
                case 5 -> extractLivelihood(trimmed);        // Current Livelihood
                case 6 -> trimmed;                           // Skills & Hobbies (free text)
                case 7 -> trimmed;                           // Aspirations (free text)
                case 8 -> trimmed;                           // Constraints (free text)
                case 9 -> extractEmploymentPref(trimmed);    // Employment Preference
                case 10 -> trimmed;                          // Region (free text)
                default -> trimmed;
            };
        }

        /** Validate that the name is at least 2 chars and not purely numeric. */
        private static String extractName(String raw) {
            if (raw.length() < 2 || raw.matches("[0-9]+")) return null;
            return raw;
        }

        /** Validate Indian mobile: starts with 6-9, exactly 10 digits. */
        private static String extractPhone(String raw) {
            String digits = raw.replaceAll("[^0-9]", "");
            if (digits.length() == 10 && "6789".indexOf(digits.charAt(0)) >= 0) return digits;
            return null;
        }

        /** Normalise education level keywords with Indic support. */
        private static String extractEducation(String raw) {
            String lower = raw.toLowerCase();
            // 10th
            if (lower.contains("tenth") || lower.contains("10th") || lower.contains("sslc") || lower.contains("class 10")
                || lower.contains("10వ") || lower.contains("10ஆம்") || lower.contains("10वीं") || lower.contains("10ನೇ")) {
                return "10th Pass";
            }
            // 12th / Inter / +2 / PUC
            if (lower.contains("twelfth") || lower.contains("12th") || lower.contains("hsc") || lower.contains("class 12")
                || lower.contains("plus two") || lower.contains("+2") || lower.contains("inter") || lower.contains("puc")
                || lower.contains("12వ") || lower.contains("12ஆம்") || lower.contains("12वीं") || lower.contains("12ನೇ")) {
                return "12th Pass";
            }
            // Graduate / Degree
            if (lower.contains("graduate") || lower.contains("degree") || lower.contains("ba") || lower.contains("bsc")
                || lower.contains("bcom") || lower.contains("b.tech") || lower.contains("பட்டதாரி") || lower.contains("డిగ్రీ")
                || lower.contains("ग्रेजुएट") || lower.contains("పట్టభద్రుడు") || lower.contains("ಪದವೀಧರ")) {
                return "Graduate";
            }
            // ITI / Diploma / Polytechnic
            if (lower.contains("iti") || lower.contains("diploma") || lower.contains("polytechnic")
                || lower.contains("ఐటిఐ") || lower.contains("டிப்ளமோ") || lower.contains("आईटीआई") || lower.contains("ಐಟಿಐ")) {
                return "ITI";
            }
            // Illiterate / None
            if (lower.contains("illiterate") || lower.contains("none") || lower.contains("no education")
                || lower.contains("లేదు") || lower.contains("இல்லை") || lower.contains("नहीं") || lower.contains("ಇಲ್ಲ")) {
                return "Illiterate / None";
            }
            return raw;
        }

        /** Normalise common livelihood descriptions with Indic support. */
        private static String extractLivelihood(String raw) {
            String lower = raw.toLowerCase();
            if (lower.contains("unemploy") || lower.contains("no work") || lower.contains("ఖాలీ")
                || lower.contains("వేరే పని లేదు") || lower.contains("வேலையற்றவர்") || lower.contains("बेरोजगार") || lower.contains("ನಿರುದ್ಯೋಗಿ")) {
                return "Unemployed";
            }
            if (lower.contains("daily wage") || lower.contains("daily wager") || lower.contains("coolie")
                || lower.contains("కూలీ") || lower.contains("கூலி") || lower.contains("दैनिक मजदूर") || lower.contains("ದಿನಗೂಲಿ")) {
                return "Daily Wage Worker";
            }
            return raw;
        }

        /** Normalise employment preference with Indic support. */
        private static String extractEmploymentPref(String raw) {
            String lower = raw.toLowerCase();
            if (lower.contains("self") || lower.contains("own business") || lower.contains("entrepreneur")
                || lower.contains("స్వయం") || lower.contains("సొంత వ్యాపారం") || lower.contains("சுய") || lower.contains("स्वरोजगार") || lower.contains("ಸ್ವ-ಉದ್ಯೋಗ")) {
                return "Self Employment";
            }
            if (lower.contains("wage") || lower.contains("job") || lower.contains("salary")
                || lower.contains("ఉద్యోగం") || lower.contains("வேலை") || lower.contains("नौकरी") || lower.contains("वेतन") || lower.contains("ಕೆಲಸ")) {
                return "Wage Employment";
            }
            if (lower.contains("both") || lower.contains("రెండూ") || lower.contains("இரண்டும்") || lower.contains("दोनों") || lower.contains("ಎರಡೂ")) {
                return "Both";
            }
            return "Both";
        }
    }

    // ========================================================================
    // LOCALIZED PROFILE & FIELD LABELS DICTIONARY
    // ========================================================================

    public static class LocalizedProfileDictionary {
        private static final Map<String, Map<String, String>> DICT = new HashMap<>();

        static {
            // English
            Map<String, String> en = new HashMap<>();
            en.put("name", "Full Name");
            en.put("phone", "Mobile Phone");
            en.put("education", "Education Qualification");
            en.put("familyOccupation", "Family / Traditional Occupation");
            en.put("currentLivelihood", "Current Livelihood");
            en.put("skills", "Skills & Hobbies");
            en.put("interests", "Career Interests");
            en.put("aspirations", "Future Aspirations");
            en.put("constraints", "Mobility / Work Constraints");
            en.put("employmentPreference", "Employment Preference");
            en.put("region", "Region / District");
            en.put("status", "Pipeline Status");
            en.put("recommendedTraining", "Recommended PM-DAKSH / NBCFDC Training");
            en.put("recommendedJobs", "Recommended Livelihood Opportunities");
            DICT.put("en-IN", en);
            DICT.put("en", en);

            // Telugu (తెలుగు)
            Map<String, String> te = new HashMap<>();
            te.put("name", "పూర్తి పేరు");
            te.put("phone", "మొబైల్ ఫోన్ నంబర్");
            te.put("education", "విద్యార్హత");
            te.put("familyOccupation", "కుటుంబ / సాంప్రదాయ వృత్తి");
            te.put("currentLivelihood", "ప్రస్తుత జీవనోపాధి");
            te.put("skills", "నైపుణ్యాలు మరియు అభిరుచులు");
            te.put("interests", "ఆసక్తి ఉన్న రంగాలు");
            te.put("aspirations", "భవిష్యత్ లక్ష్యాలు / ఆశయాలు");
            te.put("constraints", "పని మరియు ప్రయాణ పరిమితులు");
            te.put("employmentPreference", "ఉపాధి ప్రాధాన్యత");
            te.put("region", "ప్రాంతం / జిల్లా");
            te.put("status", "పైప్‌లైన్ స్థితి");
            te.put("recommendedTraining", "సిఫార్సు చేయబడిన శిక్షణా కార్యక్రమాలు");
            te.put("recommendedJobs", "సిఫార్సు చేయబడిన జీవనోపాధి అవకాశాలు");
            DICT.put("te-IN", te);
            DICT.put("te", te);

            // Tamil (தமிழ்)
            Map<String, String> ta = new HashMap<>();
            ta.put("name", "முழு பெயர்");
            ta.put("phone", "மொபைல் எண்");
            ta.put("education", "கல்வித் தகுதி");
            ta.put("familyOccupation", "குடும்ப / பாரம்பரிய தொழில்");
            ta.put("currentLivelihood", "தற்போதைய வாழ்வாதாரம்");
            ta.put("skills", "திறன்கள் மற்றும் பொழுதுபோக்குகள்");
            ta.put("interests", "தொழில் ஆர்வங்கள்");
            ta.put("aspirations", "எதிர்கால ஆசைகள்");
            ta.put("constraints", "பயண / வேலை கட்டுப்பாடுகள்");
            ta.put("employmentPreference", "வேலை விருப்பம்");
            ta.put("region", "மாவட்டம் / பகுதி");
            ta.put("status", "செயல்முறை நிலை");
            ta.put("recommendedTraining", "பரிந்துரைக்கப்பட்ட பயிற்சி திட்டங்கள்");
            ta.put("recommendedJobs", "பரிந்துரைக்கப்பட்ட வாழ்வாதார வாய்ப்புகள்");
            DICT.put("ta-IN", ta);
            DICT.put("ta", ta);

            // Hindi (हिंदी)
            Map<String, String> hi = new HashMap<>();
            hi.put("name", "पूरा नाम");
            hi.put("phone", "मोबाइल नंबर");
            hi.put("education", "शैक्षणिक योग्यता");
            hi.put("familyOccupation", "पारिवारिक / पारंपरिक व्यवसाय");
            hi.put("currentLivelihood", "वर्तमान आजीविका");
            hi.put("skills", "कौशल और शौक");
            hi.put("interests", "रुचियां");
            hi.put("aspirations", "भविष्य की आकांक्षाएं");
            hi.put("constraints", "कार्य व आवागमन बाधाएं");
            hi.put("employmentPreference", "रोजगार प्राथमिकता");
            hi.put("region", "क्षेत्र / जिला");
            hi.put("status", "पाइपलाइन स्थिति");
            hi.put("recommendedTraining", "अनुशंसित प्रशिक्षण कार्यक्रम");
            hi.put("recommendedJobs", "अनुशंसित आजीविका अवसर");
            DICT.put("hi-IN", hi);
            DICT.put("hi", hi);

            // Kannada (ಕನ್ನಡ)
            Map<String, String> kn = new HashMap<>();
            kn.put("name", "ಪೂರ್ಣ ಹೆಸರು");
            kn.put("phone", "ಮೊಬೈಲ್ ಸಂಖ್ಯೆ");
            kn.put("education", "ಶೈಕ್ಷಣಿಕ ವಿದ್ಯಾರ್ಹತೆ");
            kn.put("familyOccupation", "ಕುಟುಂಬದ / ಸಾಂಪ್ರದಾಯಿಕ ವೃತ್ತಿ");
            kn.put("currentLivelihood", "ಪ್ರಸ್ತುತ ಜೀವನೋಪಾಯ");
            kn.put("skills", "ಕೌಶಲ್ಯಗಳು ಮತ್ತು ಹವ್ಯಾಸಗಳು");
            kn.put("interests", "ಆಸಕ್ತಿಗಳು");
            kn.put("aspirations", "ಭವಿಷ್ಯದ ಆಕಾಂಕ್ಷೆಗಳು");
            kn.put("constraints", "ಕೆಲಸ ಮತ್ತು ಚಲನ ನಿರ್ಬಂಧಗಳು");
            kn.put("employmentPreference", "ಉದ್ಯೋಗ ಆದ್ಯತೆ");
            kn.put("region", "ಪ್ರದೇಶ / ಜಿಲ್ಲೆ");
            kn.put("status", "ಪೈಪ್‌ಲೈನ್ ಸ್ಥಿತಿ");
            kn.put("recommendedTraining", "ಶಿಫಾರಸು ಮಾಡಿದ ತರಬೇತಿ ಕಾರ್ಯಕ್ರಮಗಳು");
            kn.put("recommendedJobs", "ಶಿಫಾರಸು ಮಾಡಿದ ಜೀವನೋಪಾಯ ಅವಕಾಶಗಳು");
            DICT.put("kn-IN", kn);
            DICT.put("kn", kn);
        }

        public static Map<String, String> getLabels(String lang) {
            if (lang == null) return DICT.get("en-IN");
            for (String key : DICT.keySet()) {
                if (key.equalsIgnoreCase(lang) || lang.startsWith(key)) {
                    return DICT.get(key);
                }
            }
            return DICT.get("en-IN");
        }
    }

    // ========================================================================
    // AI SERVICE ABSTRACTION STUBS (ready for Whisper / Bhashini / GCP)
    // ========================================================================

    /**
     * SpeechToTextService — swap body of transcribe() to integrate real ASR.
     * Supports: Whisper API, Bhashini ASR, Google Cloud Speech-to-Text.
     */
    public static class SpeechToTextService {
        public static String transcribe(byte[] audioBytes, String languageCode) {
            return "[STT stub] Audio bytes received: " + audioBytes.length + " bytes for lang=" + languageCode;
        }
    }

    /**
     * TextToSpeechService — swap body of synthesize() to integrate real TTS.
     * Supports: Google Cloud TTS, Bhashini TTS, browser Web Speech API (default).
     */
    public static class TextToSpeechService {
        public static String synthesize(String text, String languageCode) {
            return String.format(
                "{\"text\":\"%s\",\"lang\":\"%s\",\"engine\":\"browser-tts\"}",
                text.replace("\"", "'"), languageCode
            );
        }
    }
}
