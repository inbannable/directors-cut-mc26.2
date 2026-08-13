package dev.directorscut;

import dev.directorscut.state.DirectorPersonality;
import dev.directorscut.state.DirectorState;
import dev.directorscut.state.MoodModel;
import dev.directorscut.event.EventRegistry;
import dev.directorscut.event.impl.BuiltInEvents;
import dev.directorscut.event.HintCatalog;

import java.util.Properties;
import java.util.UUID;

public final class DirectorLogicTest {
    public static void main(String[] args) {
        personalityIsDeterministicAndBounded();
        moodsReactGraduallyToContext();
        stateAndHistoryRoundTrip();
        builtInEventCatalogIsComplete();
        System.out.println("Director logic tests passed.");
    }

    private static void personalityIsDeterministicAndBounded() {
        var first = DirectorPersonality.fromSeed(123456789L);
        var second = DirectorPersonality.fromSeed(123456789L);
        assert first.equals(second);
        assert first.benevolence() >= 0 && first.benevolence() <= 1;
        assert first.cruelty() >= 0 && first.cruelty() <= 1;
        assert first.baseCooldownTicks() >= 7_200 && first.baseCooldownTicks() <= 20_400;
    }

    private static void moodsReactGraduallyToContext() {
        DirectorState state = new DirectorState(UUID.randomUUID());
        double initialBoredom = state.boredom;
        for (int i = 0; i < 30; i++) {
            MoodModel.update(state, new MoodModel.Inputs(
                    true, true, true, false, true, false, false,
                    1.0f, 20, 0, 0, 12_000));
        }
        assert state.boredom > initialBoredom;
        assert state.directorInterest > 20;
        assert state.danger >= 0 && state.danger <= 100;

        double bored = state.boredom;
        for (int i = 0; i < 25; i++) {
            MoodModel.update(state, new MoodModel.Inputs(
                    false, false, false, true, false, true, false,
                    0.7f, 14, 4, 90, 400));
        }
        assert state.boredom < bored;
        assert state.tension > 20;
    }

    private static void stateAndHistoryRoundTrip() {
        UUID player = UUID.randomUUID();
        DirectorState original = new DirectorState(player);
        original.boredom = 73.25;
        original.northChainStage = 2;
        original.history.record("gerald", 9001, true);
        Properties properties = new Properties();
        original.save(properties);

        DirectorState restored = new DirectorState(player);
        restored.load(properties);
        assert Math.abs(restored.boredom - 73.25) < 0.001;
        assert restored.northChainStage == 2;
        assert restored.history.hasSeen("gerald");
        assert restored.history.lastTriggered("gerald") == 9001;
    }

    private static void builtInEventCatalogIsComplete() {
        EventRegistry registry = new EventRegistry();
        BuiltInEvents.registerAll(registry);
        assert registry.all().size() >= 20 : "milestone requires at least 20 events";
        assert registry.get("dont_go_north") != null;
        assert registry.get("mercy") != null;
        assert registry.get("gerald") != null;
        assert registry.get("perfect_view") != null;
        for (var event : registry.all()) {
            assert HintCatalog.hasHint(event.id()) : "missing hint for " + event.id();
            assert !HintCatalog.hintFor(event.id(), 0).isBlank();
        }
    }
}
