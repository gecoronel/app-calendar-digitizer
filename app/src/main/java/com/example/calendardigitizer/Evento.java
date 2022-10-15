package com.example.calendardigitizer;

import java.util.List;

public class Evento {

    private int mes;
    private String color;
    private List<Integer> dias;

    public Evento(){}

    public void setMes(int mes) {
        this.mes = mes;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public void setDias(List<Integer> dias) {
        this.dias = dias;
    }

    public int getMes() {
        return mes;
    }

    public String getColor() {
        return color;
    }

    public List<Integer> getDias() {
        return dias;
    }

}