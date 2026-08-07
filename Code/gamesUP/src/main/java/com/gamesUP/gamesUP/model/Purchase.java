package com.gamesUP.gamesUP.model;

import java.util.Date;
import java.util.List;

public class Purchase {

	
	List<PurchaseLine> line;
	Date date;
	boolean paid;
	boolean delivered;
	boolean archived;
}
