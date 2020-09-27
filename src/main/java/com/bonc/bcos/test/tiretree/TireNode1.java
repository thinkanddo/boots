package com.bonc.bcos.test.tiretree;

import java.util.HashMap;
import java.util.Map;

public class TireNode1 {
    boolean isLeaf;
    Map<Character,TireNode1> children;

    public TireNode1(){
        this.isLeaf = false;          // init false
        children = new HashMap<>();   // don't forget it
    }

    /**
     * Insert
     */
    public  void insert(TireNode1 node,String str){
        if(str == null || str.length()<=0){
            return;
        }
        for(int i =0 ; i<str.length() ; i++){
            char ch = str.charAt(i);
            if(node.children.containsKey(ch)){           // 如果查找到，则继续向下查找。
                node = node.children.get(ch);       // 把children的TireNode1  赋值给 node
            }else {
                TireNode1 child = new TireNode1();
                node.children.put(ch,child);
                node = node.children.get(ch);
            }
        }
        node.isLeaf = true;
    }

    /**
     * Search
     */
    public boolean search(TireNode1 node,String req){
        if(req == null || req.isEmpty()){
            return false;
        }
        for(int i=0 ; i<req.length() ; i++){
            char ch = req.charAt(i);
            if(!node.children.containsKey(ch)){
                return  false;
            }else {
                node = node.children.get(ch);
            }
        }
        return node.isLeaf == true ;
    }

    public static void main(String[] args) {
        TireNode1 node = new TireNode1();
        node.insert(node,"asdfghaa");
        boolean flag = node.search(node,"asd");
        System.out.println(node );
        System.out.println(flag);
    }

}
