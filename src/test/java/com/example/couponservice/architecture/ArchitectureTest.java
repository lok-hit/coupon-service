package com.example.couponservice.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.RestController;

import java.nio.file.Paths;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * ArchUnit tests enforcing hexagonal architecture boundaries.
 *
 * <p>Production classes are loaded from {@code build/classes/archunit}, which is
 * produced by the {@code compileForArchUnit} Gradle task.  That task compiles
 * the main sources to Java 21 class-file format so ArchUnit's bundled ASM can
 * parse them.  Files that rely on Java 25 preview features
 * ({@code RedeemCouponService}, {@code DomainConfiguration}) are intentionally
 * excluded from that task and from these checks; the remaining source tree fully
 * represents the architecture.
 */
class ArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void importClasses() {
        String projectDir = System.getProperty("user.dir");
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPath(Paths.get(projectDir, "build", "classes", "archunit"));
    }

    /** Sanity check — fails fast if the compileForArchUnit task did not run. */
    @Test
    void archunit_finds_production_classes() {
        assertThat(importedClasses)
                .as("ArchUnit must find classes in build/classes/archunit; "
                        + "ensure the compileForArchUnit task ran before this test")
                .isNotEmpty();
    }

    @Test
    void domainMustNotDependOnInfrastructure() {
        ArchRule rule = noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .because("domain must remain independent of infrastructure adapters");
        rule.allowEmptyShould(true).check(importedClasses);
    }

    @Test
    void domainMustNotDependOnApi() {
        ArchRule rule = noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("..api..")
                .because("domain must not depend on the API layer");
        rule.allowEmptyShould(true).check(importedClasses);
    }

    @Test
    void apiMustNotDependOnInfrastructure() {
        ArchRule rule = noClasses().that().resideInAPackage("..api..")
                .should().dependOnClassesThat().resideInAPackage("..infrastructure..")
                .because("API layer must talk to domain ports only, never directly to infrastructure");
        rule.allowEmptyShould(true).check(importedClasses);
    }

    @Test
    void infrastructureMustNotDependOnApi() {
        ArchRule rule = noClasses().that().resideInAPackage("..infrastructure..")
                .should().dependOnClassesThat().resideInAPackage("..api..")
                .because("infrastructure adapters must not depend on the API layer");
        rule.allowEmptyShould(true).check(importedClasses);
    }

    @Test
    void domainMustNotDependOnSpring() {
        ArchRule rule = noClasses().that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().resideInAPackage("org.springframework..")
                .because("domain model must be a plain-Java, framework-free core");
        rule.allowEmptyShould(true).check(importedClasses);
    }

    @Test
    void controllersMustBeAnnotatedWithRestController() {
        ArchRule rule = classes().that().resideInAPackage("..api.controller..")
                .should().beAnnotatedWith(RestController.class)
                .because("every class in the controller package is a REST controller");
        rule.allowEmptyShould(true).check(importedClasses);
    }

    @Test
    void useCasesMustBeInterfacesInDomainPortIn() {
        ArchRule rule = classes().that().resideInAPackage("..domain.port.in..")
                .and().areTopLevelClasses()
                .should().beInterfaces()
                .because("input ports are defined as interfaces to keep domain logic decoupled from callers");
        rule.allowEmptyShould(true).check(importedClasses);
    }
}
