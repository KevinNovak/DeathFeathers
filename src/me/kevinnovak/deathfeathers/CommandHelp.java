package me.kevinnovak.deathfeathers;

import java.util.ArrayList;

import org.bukkit.entity.Player;

public class CommandHelp {
	private ArrayList<String> lines = new ArrayList<String>();
	private Player player = null;
	private ColorConverter colorConv = null;
	
	public CommandHelp(Player player, ColorConverter colorConv) {
		this.player = player;
		this.colorConv = colorConv;
	}
	
	private void evaluate() {
		if (player.hasPermission("deathfeathers.command")) {
			lines.add(colorConv.convertConfig("helpFindDeath"));
		}
		if (player.hasPermission("deathfeathers.tp")) {
			lines.add(colorConv.convertConfig("helpTpDeath"));
		}
		if (player.hasPermission("deathfeathers.tp.others")) {
			lines.add(colorConv.convertConfig("helpTpDeathOther"));
		}
	}
	public void print(int pageNum) {
		this.evaluate();
		if (pageNum > Math.ceil((double)lines.size()/7)) {
			pageNum = 1;
		}
		player.sendMessage(colorConv.convert(colorConv.convertConfig("helpHeader")));
		if (!lines.isEmpty()) {
			for (int i=7*(pageNum-1); i<lines.size() && i<(7*pageNum); i++) {
				player.sendMessage(colorConv.convert(lines.get(i)));
			}
			if (lines.size() > 7*pageNum) {
				int nextPageNum = pageNum + 1;
				player.sendMessage(colorConv.convertConfig("helpPage").replace("{PAGE}", Integer.toString(nextPageNum)));
			}
		} else {
			player.sendMessage(colorConv.convertConfig("helpNoCommands"));
		}
		player.sendMessage(colorConv.convert(colorConv.convertConfig("helpFooter")));
	}
}