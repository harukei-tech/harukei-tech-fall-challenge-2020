import java.util.*;
import java.io.*;
import java.math.*;
import java.util.Collections;
import java.util.List;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {

    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int playerCount = 2;

        // game loop
        while (true) {

            int actionCount = in.nextInt(); // the number of spells and recipes in play
            List<Product> products = new ArrayList<>();  //とりあえずactionCount分作る

            for (int i = 0; i < actionCount; i++) {
                int actionId = in.nextInt(); // the unique ID of this spell or recipe
                String actionType = in.next(); // in the first league: BREW; later: CAST, OPPONENT_CAST, LEARN, BREW
                int delta0 = in.nextInt(); // tier-0 ingredient change
                int delta1 = in.nextInt(); // tier-1 ingredient change
                int delta2 = in.nextInt(); // tier-2 ingredient change
                int delta3 = in.nextInt(); // tier-3 ingredient change
                int price = in.nextInt(); // the price in rupees if this is a potion
                int tomeIndex = in.nextInt(); // in the first two leagues: always 0; later: the index in the tome if this is a tome spell, equal to the read-ahead tax; For brews, this is the value of the current urgency bonus
                int taxCount = in.nextInt(); // in the first two leagues: always 0; later: the amount of taxed tier-0 ingredients you gain from learning this spell; For brews, this is how many times you can still gain an urgency bonus
                boolean castable = in.nextInt() != 0; // in the first league: always 0; later: 1 if this is a castable player spell
                boolean repeatable = in.nextInt() != 0; // for the first two leagues: always 0; later: 1 if this is a repeatable player spell
                products.add(new Product(actionId, actionType, delta0, delta1, delta2, delta3, price, tomeIndex, taxCount, castable, repeatable));
            }

            User[] users = new User[playerCount];
            for (int i = 0; i < playerCount; i++) {
                int inv0 = in.nextInt(); // tier-0 ingredients in inventory
                int inv1 = in.nextInt();
                int inv2 = in.nextInt();
                int inv3 = in.nextInt();
                int score = in.nextInt(); // amount of rupees
                users[i] = new User(inv0,inv1,inv2,inv3,score);
            }

            //自分が作れるか
            for(Product product: products) {
                product.setCreatable(users[0]);
            }

            Collections.sort(products);

            // Write an action using System.out.println()
            // To debug: System.err.println("Debug messages...");
            System.err.println(products);
            for(Product product: products) {
                System.err.println(products.get(0).actionId);
            }

            // in the first league: BREW <id> | WAIT; later: BREW <id> | CAST <id> [<times>] | LEARN <id> | REST | WAIT
            System.out.println("BREW " + products.get(0).actionId);
        }
    }

}

class Product implements Comparable<Product>
{
    int price;
    int actionId;
    String actionType;
    int delta0; //消費数。(元データはマイナスなので、プラスに変換して保持している)
    int delta1;
    int delta2;
    int delta3;
    int totalRequestNumber;

    boolean creatable;

    public Product(int actionId, String actionType, int delta0,int delta1,int delta2,int delta3,int price, int tomeIndex, int taxCount, boolean castable, boolean repeatable){
        this.actionId = actionId;
        this.actionType = actionType;
        this.delta0 = -delta0;
        this.delta1 = -delta1;
        this.delta2 = -delta2;
        this.delta3 = -delta3;
        this.totalRequestNumber = this.delta0 + this.delta1 + this.delta2 + this.delta3;
        this.price = price;
    }


    public void setCreatable(User user)
    {
        this.creatable = (this.delta0 <= user.delta0)
                && (this.delta1 <= user.delta1)
                && (this.delta2 <= user.delta2)
                && (this.delta3 <= user.delta3);
    }


    @Override
    public int compareTo(Product o) {
        //作れないものは比較対象外最優先
        if(this.creatable != o.creatable) {
            if(this.creatable) {
                return -1;
            }
            return 1;
        }

        //スコアの高い順の貪欲法
        if(o.price < this.price) {
            return -1;
        } else if(o.price > this.price){
            return 1;
        }

        //同スコアならコスパがいいものを優先
        if(o.totalRequestNumber == this.totalRequestNumber) {
            return 0;
        } else if (this.totalRequestNumber <= o.totalRequestNumber) {
            return -1;
        } else {
            return 1;
        }
    }
}


class User
{
    int score;
    int delta0;
    int delta1;
    int delta2;
    int delta3;

    public User(int delta0,int delta1,int delta2,int delta3, int score) {
        this.delta0 = delta0;
        this.delta1 = delta1;
        this.delta2 = delta2;
        this.delta3 = delta3;
        this.score = score;
    }

}