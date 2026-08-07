package com.gamesUP.gamesUP.controller;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import org.springframework.web.bind.annotation.*;

import com.gamesUP.gamesUP.model.Game;

public class GameController {

    private String jdbcUrl = "jdbc:mysql://localhost:3306/gameUP";
    private String username = "root";
    private String password = "password";

    @GetMapping
    public List<Game> getAllJeux() {
        List<Game> jeux = new ArrayList<>();
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
             Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT * FROM jeux")) {

            while (rs.next()) {
                Game game = new Game();
                game.id=rs.getInt("id");
                game.nom = rs.getString("nom");
                game.auteur=rs.getString("auteur");
                jeux.add(game);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return jeux;
    }

    @PostMapping
    public void ajouterJeu(@RequestBody Game game) {
        try (Connection connection = DriverManager.getConnection(jdbcUrl, username, password);
             PreparedStatement stmt = connection.prepareStatement("INSERT INTO jeux (nom, auteur) VALUES (?, ?)")) {

            stmt.setString(1, game.nom);
            stmt.setString(2, game.auteur);
            stmt.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}