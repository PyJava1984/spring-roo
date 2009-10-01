package org.springframework.roo.classpath.operations;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import org.springframework.roo.classpath.PhysicalTypeCategory;
import org.springframework.roo.classpath.PhysicalTypeIdentifier;
import org.springframework.roo.classpath.details.ClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.DefaultClassOrInterfaceTypeDetails;
import org.springframework.roo.classpath.details.annotations.AnnotationAttributeValue;
import org.springframework.roo.classpath.details.annotations.AnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.DefaultAnnotationMetadata;
import org.springframework.roo.classpath.details.annotations.StringAttributeValue;
import org.springframework.roo.model.JavaSymbolName;
import org.springframework.roo.model.JavaType;
import org.springframework.roo.model.ReservedWords;
import org.springframework.roo.project.Path;
import org.springframework.roo.shell.CliAvailabilityIndicator;
import org.springframework.roo.shell.CliCommand;
import org.springframework.roo.shell.CliOption;
import org.springframework.roo.shell.CommandMarker;
import org.springframework.roo.support.lifecycle.ScopeDevelopmentShell;
import org.springframework.roo.support.util.Assert;

/**
 * Shell commands for {@link ClasspathOperations}.
 * 
 * @author Ben Alex
 * @since 1.0
 *
 */
@ScopeDevelopmentShell
public class ClasspathCommands implements CommandMarker {
	private ClasspathOperations classpathOperations;

	public ClasspathCommands(ClasspathOperations classpathOperations) {
		Assert.notNull(classpathOperations, "Classpath operations required");
		this.classpathOperations = classpathOperations;
	}
	
	@CliAvailabilityIndicator({"class", "dod", "test integration"})
	public boolean isProjectAvailable() {
		return classpathOperations.isProjectAvailable();
	}
	
	@CliAvailabilityIndicator({"entity"})
	public boolean isPersistentClassAvailable() {
		return classpathOperations.isPersistentClassAvailable();
	}
	
	@CliCommand(value="class", help="Creates a new Java source file in any project path")
	public void createType(
			@CliOption(key="name", mandatory=true) JavaType name, 
			@CliOption(key="path", mandatory=true) Path path, 
			@CliOption(key="extends", mandatory=false, unspecifiedDefaultValue="java.lang.Object", help="The superclass (defaults to java.lang.Object)") JavaType superclass,
			@CliOption(key="abstract", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="Whether the generated class should be marked as abstract") boolean createAbstract,
			@CliOption(key="permitReservedWords", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {
		
		if (!permitReservedWords) {
			ReservedWords.verifyReservedWordsNotPresent(name);
		}
		
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(name, path);
		
		List<JavaType> extendsTypes = new ArrayList<JavaType>();
		extendsTypes.add(superclass);
		
		int modifier = Modifier.PUBLIC;
		if (createAbstract) {
			modifier = modifier |= Modifier.ABSTRACT;
		}
		ClassOrInterfaceTypeDetails details = new DefaultClassOrInterfaceTypeDetails(declaredByMetadataId, name, modifier, PhysicalTypeCategory.CLASS, null, null, null, classpathOperations.getSuperclass(superclass), extendsTypes, null, null, null);
		classpathOperations.generateClassFile(details);
	}
	
	@CliCommand(value="dod", help="Creates a new data on demand for the specified entity")
	public void newDod(
			@CliOption(key="entity", mandatory=false, unspecifiedDefaultValue="*", optionContext="update,project") JavaType entity,
			@CliOption(key="permitReservedWords", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {
		
		if (!permitReservedWords) {
			ReservedWords.verifyReservedWordsNotPresent(entity);
		}

		classpathOperations.newDod(entity);
	}

	@CliCommand(value="test integration", help="Creates a new integration test for the specified entity")
	public void newIntegrationTest(
			@CliOption(key="entity", mandatory=false, unspecifiedDefaultValue="*", optionContext="update,project") JavaType entity,
			@CliOption(key="permitReservedWords", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {
		
		if (!permitReservedWords) {
			ReservedWords.verifyReservedWordsNotPresent(entity);
		}

		classpathOperations.newIntegrationTest(entity);
	}

	@CliCommand(value="entity", help="Creates a new JPA persistent entity in SRC_MAIN_JAVA")
	public void newPersistenceClassJpa(
			@CliOption(key="name", optionContext="update,project", mandatory=true) JavaType name, 
			@CliOption(key="extends", mandatory=false, unspecifiedDefaultValue="java.lang.Object", help="The superclass (defaults to java.lang.Object)") JavaType superclass,
			@CliOption(key="abstract", mandatory=false, specifiedDefaultValue="true", unspecifiedDefaultValue="false", help="Whether the generated class should be marked as abstract") boolean createAbstract,
			@CliOption(key="testAutomatically", mandatory=false, specifiedDefaultValue="true", unspecifiedDefaultValue="false", help="Create automatic integration tests for this entity") boolean testAutomatically,
			@CliOption(key="table", mandatory=false, help="The JPA table name to use for this entity") String table,
			@CliOption(key="identifierField", mandatory=false, help="The JPA identifier field name to use for this entity") String identifierField,
			@CliOption(key="identifierColumn", mandatory=false, help="The JPA identifier field column to use for this entity") String identifierColumn,
			@CliOption(key="permitReservedWords", mandatory=false, unspecifiedDefaultValue="false", specifiedDefaultValue="true", help="Indicates whether reserved words are ignored by Roo") boolean permitReservedWords) {
		
		if (!permitReservedWords) {
			ReservedWords.verifyReservedWordsNotPresent(name);
		}
		
		// Reject attempts to name the entity "Test", due to possible clashes with data on demand (see ROO-50)
		if (name.getSimpleTypeName().startsWith("Test") || name.getSimpleTypeName().endsWith("TestCase") || name.getSimpleTypeName().endsWith("Test")) {
			throw new IllegalArgumentException("Entity name rejected as conflicts with test execution defaults; please remove 'Test' and/or 'TestCase'");
		}
		
		// Produce entity itself
		String declaredByMetadataId = PhysicalTypeIdentifier.createIdentifier(name, Path.SRC_MAIN_JAVA);
		List<AnnotationMetadata> entityAnnotations = new ArrayList<AnnotationMetadata>();
		entityAnnotations.add(new DefaultAnnotationMetadata(new JavaType("javax.persistence.Entity"), new ArrayList<AnnotationAttributeValue<?>>()));
		entityAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.roo.addon.javabean.RooJavaBean"), new ArrayList<AnnotationAttributeValue<?>>()));
		entityAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.roo.addon.tostring.RooToString"), new ArrayList<AnnotationAttributeValue<?>>()));

		List<AnnotationAttributeValue<?>> entityAttrs = new ArrayList<AnnotationAttributeValue<?>>();
		if (identifierField != null) {
			entityAttrs.add(new StringAttributeValue(new JavaSymbolName("identifierField"), identifierField));
		}
		if (identifierColumn != null) {
			entityAttrs.add(new StringAttributeValue(new JavaSymbolName("identifierColumn"), identifierColumn));
		}
		entityAnnotations.add(new DefaultAnnotationMetadata(new JavaType("org.springframework.roo.addon.entity.RooEntity"), entityAttrs));

		
		if (table != null) {
			List<AnnotationAttributeValue<?>> attrs = new ArrayList<AnnotationAttributeValue<?>>();
			attrs.add(new StringAttributeValue(new JavaSymbolName("name"), table));
			entityAnnotations.add(new DefaultAnnotationMetadata(new JavaType("javax.persistence.Table"), attrs));
		}
		
		List<JavaType> extendsTypes = new ArrayList<JavaType>();
		extendsTypes.add(superclass);

		int modifier = Modifier.PUBLIC;
		if (createAbstract) {
			modifier = modifier |= Modifier.ABSTRACT;
		}
		ClassOrInterfaceTypeDetails details = new DefaultClassOrInterfaceTypeDetails(declaredByMetadataId, name, modifier, PhysicalTypeCategory.CLASS, null, null, null, classpathOperations.getSuperclass(superclass), extendsTypes, null, entityAnnotations, null);
		classpathOperations.generateClassFile(details);
		
		if (testAutomatically) {
			classpathOperations.newIntegrationTest(name);
		}
		
		
	}

}
