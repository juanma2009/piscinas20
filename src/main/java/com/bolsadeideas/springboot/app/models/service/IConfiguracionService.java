package com.bolsadeideas.springboot.app.models.service;



public interface IConfiguracionService {


    public String getValor(String clave);

    public void setValor(String clave, String valor);

    public void eliminar(String clave);

    public void guardar(String clave, String valor);

    public void eliminarTodo();

    public void guardarTodo();
}
