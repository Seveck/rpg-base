package com.rpg;

import com.rpg.controller.HeroeController;
import com.rpg.dao.HeroeDAO;
import com.rpg.view.HeroeUI;

public class Main {

    public static void main(String[] args) {
        HeroeDAO heroeDAO = new HeroeDAO();
        HeroeController controller = new HeroeController(heroeDAO);
        HeroeUI vista = new HeroeUI(controller);
        vista.iniciar();
    }
}
