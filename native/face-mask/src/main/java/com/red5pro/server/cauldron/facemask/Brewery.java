package com.red5pro.server.cauldron.facemask;

import java.util.Map;
//com.red5pro.server.cauldron.facemask.Brewery
public class Brewery {
	private String potion;
	private Map<String,Object> ingredients;
	
	public String getPotion() {
		return potion;
	}
	public void setPotion(String name) {
		this.potion = name;
	}
	public Map<String, Object> getIngredients() {
		return ingredients;
	}
	public void setIngredients(Map<String, Object> ingredients) {
		this.ingredients = ingredients;
	}

}
