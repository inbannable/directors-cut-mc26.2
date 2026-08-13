package dev.directorscut.state;

public final class MoodModel {
    private MoodModel() {
    }

    public record Inputs(
            boolean underground,
            boolean dark,
            boolean stationary,
            boolean exploring,
            boolean mining,
            boolean inCombat,
            boolean nearHome,
            float healthRatio,
            int food,
            int hostiles,
            double recentTravel,
            long ticksSinceEvent
    ) {
    }

    public static void update(DirectorState state, Inputs in) {
        double boredomTarget = 8;
        if (in.stationary()) boredomTarget += 30;
        if (in.mining()) boredomTarget += 28;
        if (in.recentTravel() < 8) boredomTarget += 18;
        if (in.exploring() || in.inCombat()) boredomTarget -= 35;

        double dangerTarget = (1.0 - in.healthRatio()) * 62;
        if (in.food() <= 6) dangerTarget += 20;
        dangerTarget += Math.min(28, in.hostiles() * 4.5);
        if (in.underground() && in.dark()) dangerTarget += 12;

        double tensionTarget = dangerTarget * 0.6;
        if (in.inCombat()) tensionTarget += 35;
        if (in.dark() && in.underground()) tensionTarget += 10;

        double curiosityTarget = in.exploring() ? 72 : 18;
        if (in.recentTravel() > 70) curiosityTarget += 12;

        double comfortTarget = in.nearHome() ? 70 : 14;
        if (in.healthRatio() > 0.8 && in.food() > 15 && in.hostiles() == 0) comfortTarget += 15;

        state.boredom = approach(state.boredom, boredomTarget, 0.055);
        state.danger = approach(state.danger, dangerTarget, 0.13);
        state.tension = approach(state.tension, tensionTarget, 0.09);
        state.curiosity = approach(state.curiosity, curiosityTarget, 0.055);
        state.comfort = approach(state.comfort, comfortTarget, 0.045);
        state.mystery = clamp(state.mystery * 0.9985);
        state.directorInterest = clamp(Math.max(
                approach(state.directorInterest, Math.min(100, in.ticksSinceEvent() / 240.0), 0.045),
                state.boredom * 0.55 + state.curiosity * 0.18
        ));
    }

    public static double approach(double current, double target, double rate) {
        return clamp(current + (target - current) * rate);
    }

    public static double clamp(double value) {
        return Math.max(0.0, Math.min(100.0, value));
    }
}
