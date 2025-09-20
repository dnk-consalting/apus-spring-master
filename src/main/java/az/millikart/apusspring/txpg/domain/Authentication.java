/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package az.millikart.apusspring.txpg.domain;

class Authentication {

    private Cvv2Block cvv2Block;

    public Authentication(Cvv2Block cvv2Block) {
        this.cvv2Block = cvv2Block;
    }
    
    public Authentication() {
        this.cvv2Block = new Cvv2Block();
    }

    public void setCvv2Block(String data) {
        this.cvv2Block.data = data;
    }

    public Cvv2Block getCvv2Block() {
        return cvv2Block;
    }

    public void setCvv2Block(Cvv2Block cvv2Block) {
        this.cvv2Block = cvv2Block;
    }

    
  
    private static class Cvv2Block {

        private String data;

        public Cvv2Block() {
        }

        public Cvv2Block(String data) {
            this.data = data;
        }
        
        

        public String getData() {
            return data;
        }

        public void setData(String data) {
            this.data = data;
        }
    }
}
