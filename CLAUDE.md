# 🔍 Build and Verification Commands

## Compilation

```bash
# Navigate to project directory
cd C:\Users\Juanma\IdeaProjects\joyeria

# Full compilation with all tests
mvn clean compile

# Quick compilation without tests
mvn compile -DskipTests

# Check for style/lint issues (if configured)
mvn checkstyle:check

# Type checking (if using any type-checking plugins)
mvn help:active-profiles
```

## Verification

After making changes, run these to ensure everything works:

```bash
# Lint/Code Quality
mvn clean compile -DskipTests

# Unit Tests (if available)
mvn test

# Build JAR
mvn package -DskipTests

# Run application (if it's a runnable app)
mvn spring-boot:run
```

## Specific to This Implementation

```bash
# After updating PedidoController.java and pedidoform.html
mvn clean compile -DskipTests

# If there are specific test files for the controller
mvn test -Dtest=PedidoControllerTest
```

## Expected Output

```
[INFO] ----
[INFO] BUILD SUCCESS
[INFO] ----
[INFO] Total time: X.XXXs
[INFO] Finished at: YYYY-MM-DDTHH:MM:SS+01:00
```

---

**Note**: If compilation fails, check:
1. Java version compatibility (check `pom.xml` for `<source>` and `<target>`)
2. JAVA_HOME environment variable is set correctly
3. All dependencies are downloaded (sometimes network issues)
4. Check error messages for specific import or syntax issues
