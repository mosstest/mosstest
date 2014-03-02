package net.mosstest.launcher;

public class SingleplayerBindingBean {

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGame() {
        return game;
    }

    public void setGame(String game) {
        this.game = game;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    private String name;
    private String game;
    private String desc;

    public SingleplayerBindingBean(String name, String game, String desc) {
        this.name = name;
        this.game = game;
        this.desc = desc;
    }

    public SingleplayerBindingBean() {
    }
}