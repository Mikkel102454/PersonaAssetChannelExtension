package nu.miguel.persona.assetchannel;

import nu.miguel.persona.Main;
import nu.miguel.persona.api.PersonaApi;
import nu.miguel.persona.api.BuiltinExpansion;
import nu.miguel.persona.content.Content;
import nu.miguel.persona.content.ContentLoader;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.yaml.snakeyaml.Yaml;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class ExamplesTest {
    @TempDir Path contentRoot;

    @Test void completeExamplesLoadThroughPersona() throws Exception {
        copy("quests/audio-tour.yml.example", "quests/audio-tour.yml");
        copy("npcs/sound-guide.yml.example", "npcs/sound-guide.yml");
        copy("dialogues/sound-guide-dialogue.yml.example", "dialogues/sound-guide-dialogue.yml");

        PersonaApi api = new PersonaApi(mock(Main.class));
        assertTrue(api.register(new BuiltinExpansion()));
        assertTrue(api.register(new AssetChannelExpansion(new ExpansionBehaviorTest.StubAdapter())));
        Content.Registry registry = new ContentLoader(contentRoot.toFile(), Duration.ZERO, api).load();

        assertTrue(registry.quests().containsKey("demo:audio-tour"));
        assertTrue(registry.npcs().containsKey("demo:sound-guide"));
        assertTrue(registry.dialogues().containsKey("demo:sound-guide-dialogue"));
    }

    @Test void commandSnippetFileIsValidYamlList() {
        String text = assertDoesNotThrow(() -> Files.readString(Path.of("examples/command-snippets.yml.example")));
        assertInstanceOf(java.util.List.class, new Yaml().load(text));
        assertTrue(text.contains("assetchannel:play-sound"));
        assertTrue(text.contains("assetchannel:stop-soundtrack"));
    }

    private void copy(String source, String target) throws Exception {
        Path destination = contentRoot.resolve(target);
        Files.createDirectories(destination.getParent());
        Files.copy(Path.of("examples").resolve(source), destination);
    }
}
