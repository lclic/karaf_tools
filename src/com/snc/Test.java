package com.snc;

/**
 * Created by LC on 2016/11/6.
 */
public class Test {

    public static void main(String[] args) {

        App a = new App();
        a.p.printName();

        SubApp sa = new SubApp();
        sa.p.printName();

        App aa = (App)sa;
        aa.p.printName();
    }

}

class App {
    Person p = new Person("test");
}

class SubApp extends App{
    Person p = new Person("testSub");
}

class Person{
    private String name;

    public Person(String name){
        this.name = name;
    }

    public void printName(){
        System.out.println(this.name);
    }

}