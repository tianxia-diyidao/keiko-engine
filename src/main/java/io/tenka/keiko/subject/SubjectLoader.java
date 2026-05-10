package io.tenka.keiko.subject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Loads the active {@link Subject} at boot from
 * {@code classpath:subjects/<id>/subject.toml}.
 *
 * <p>Subject id is read from {@code keiko.subject} property (which
 * defaults to {@code STUDY_SUBJECT} env var, then to {@code us-conlaw}).
 * Mirrors the Python sibling's {@code flashcards.subjects.active()} cache
 * — single load per process.
 */
@Configuration
public class SubjectLoader {

    private static final Logger log = LoggerFactory.getLogger(SubjectLoader.class);

    private final TomlMapper tomlMapper = new TomlMapper();

    @Bean
    public Subject activeSubject(@Value("${keiko.subject:us-conlaw}") String subjectId) {
        String manifestPath = "subjects/" + subjectId + "/subject.toml";
        Resource manifestResource = new ClassPathResource(manifestPath);

        if (!manifestResource.exists()) {
            String available = listAvailableSubjects();
            throw new IllegalStateException(
                    "Subject '" + subjectId + "' not found at classpath:" + manifestPath
                            + ". Available subjects under classpath:subjects/: " + available
                            + ". Set STUDY_SUBJECT (or keiko.subject) to one of those, or add a "
                            + "new subject directory with a subject.toml manifest under "
                            + "src/main/resources/subjects/<id>/."
            );
        }

        try (InputStream in = manifestResource.getInputStream()) {
            @SuppressWarnings("unchecked")
            Map<String, Object> manifest = tomlMapper.readValue(in, Map.class);
            Subject subject = new Subject(
                    str(manifest, "id", subjectId),
                    str(manifest, "name", "Untitled Subject"),
                    str(manifest, "brand_mark", ""),
                    str(manifest, "subhead", ""),
                    str(manifest, "primary_language", "en"),
                    str(manifest, "default_ui_language", "en"),
                    stringList(manifest.get("explanation_languages")),
                    stringList(manifest.get("default_deck_filter")),
                    str(manifest, "exam_date", ""),
                    "subjects/" + subjectId + "/cards.json"
            );
            log.info("Loaded active subject: {}", subject);
            return subject;
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failed to parse subject manifest at classpath:" + manifestPath, e);
        }
    }

    private static String str(Map<String, Object> m, String key, String fallback) {
        Object v = m.get(key);
        return v == null ? fallback : v.toString();
    }

    @SuppressWarnings("unchecked")
    private static List<String> stringList(Object v) {
        if (v == null) return List.of();
        if (v instanceof List<?> list) {
            return list.stream().map(Object::toString).toList();
        }
        return List.of(v.toString());
    }

    /** Pull '<id>' out of a 'classpath:subjects/<id>/subject.toml' Resource.
     *  Wrapped in its own method because Resource.getURL() throws IOException
     *  and lambdas can't propagate checked exceptions through Stream.map(). */
    private static String extractSubjectId(Resource r) {
        try {
            String url = r.getURL().toString();
            int sIdx = url.indexOf("/subjects/");
            int eIdx = url.indexOf("/subject.toml");
            if (sIdx < 0 || eIdx < 0) return "?";
            return url.substring(sIdx + "/subjects/".length(), eIdx);
        } catch (IOException e) {
            return "?";
        }
    }

    private String listAvailableSubjects() {
        try {
            ResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] hits = resolver.getResources("classpath:subjects/*/subject.toml");
            return Arrays.stream(hits)
                    .map(SubjectLoader::extractSubjectId)
                    .distinct()
                    .sorted()
                    .collect(Collectors.joining(", ", "[", "]"));
        } catch (IOException e) {
            return "[unable to enumerate: " + e.getMessage() + "]";
        }
    }
}
