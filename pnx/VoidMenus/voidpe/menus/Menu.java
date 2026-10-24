package voidpe.menus;

import java.util.ArrayList;
import java.util.List;

/** Одно меню: либо сундук с предметами (items), либо форма с кнопками (buttons). */
public final class Menu {

    public String id = "";
    public String title = "";
    public String permission = "";
    public int size = 54;
    public boolean form = false;

    /** тексты формы (только для type=form) */
    public String content = "";

    public final List<String> openCommands = new ArrayList<>();
    public final List<MenuItem> items = new ArrayList<>();
    public final List<Button> buttons = new ArrayList<>();
    public final List<Action> openActions = new ArrayList<>();

    public static final class MenuItem {
        public String name = "";
        public String material = "PAPER";
        public int amount = 1;
        public String displayName = "";
        public final List<String> lore = new ArrayList<>();
        public final List<Integer> slots = new ArrayList<>();
        public final List<Action> actions = new ArrayList<>();
    }

    public static final class Button {
        public String text = "";
        public String image = "";
        public String permission = "";
        public final List<Action> actions = new ArrayList<>();
    }

    public static final class Action {
        public final String type;
        public final String value;

        public Action(String type, String value) {
            this.type = type == null ? "" : type.trim().toLowerCase(java.util.Locale.ROOT);
            this.value = value == null ? "" : value;
        }
    }
}
