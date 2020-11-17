import java.util.*;
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
        int playerId = 0;
        int upperProductCount = 3;
        int currentProductCount = 0;
        boolean isLastSpurt = false;

        // game loop
        while (true) {

            int actionCount = in.nextInt(); // the number of spells and recipes in play
            List<Product> products = new ArrayList<>();

            //javaではオブジェクトの配列が作れないらしい
            List<Spell> mySpells = new ArrayList<>();
            List<Spell> opponentSpells = new ArrayList<>();

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
                switch (actionType) {
                    case "CAST":
                        mySpells.add(new Spell(actionId, delta0, delta1, delta2, delta3, castable));
                        break;
                    case "OPPONENT_CAST":
                        opponentSpells.add(new Spell(actionId, delta0, delta1, delta2, delta3, castable));
                        break;
                    case "BREW":
                        products.add(new Product(actionId, actionType, delta0, delta1, delta2, delta3, price, tomeIndex, taxCount, castable, repeatable));
                        break;
                    default:
                        break;
                }
            }

            User[] users = new User[playerCount];
            for (int i = 0; i < playerCount; i++) {
                int inv0 = in.nextInt(); // tier-0 ingredients in inventory
                int inv1 = in.nextInt();
                int inv2 = in.nextInt();
                int inv3 = in.nextInt();
                int score = in.nextInt(); // amount of rupees
                users[i] = new User(inv0, inv1, inv2, inv3, score);
            }
            users[playerId].setSpells(mySpells);
            users[1].setSpells(opponentSpells);

            //自分が作る場合の優先順位を設定
            for (Product product : products) {
                product.setStepToCreate(users[playerId]);
                product.setPriority(isLastSpurt, users[0], users[1]);
            }

            Collections.sort(products);

            //作りたいものとプレイヤーの行動をセット
            Command command = null;
            for (Product product : products) {
                users[playerId].setTargetProduct(product);
                try {
                    command = users[playerId].getCommandToDo();
                    break;
                } catch (NothingCanDoException e) {
                    //何もできず詰んだ場合は作るプロダクトを変える
                    continue;
                }
            }
            //どうしようもなくなったらずっと休む
            if (command == null) {
                command = new Rest();
            }

            //BREWする場合それを保持(ループで消えてしまうにも拘わらず入力データに無いための処置。相手の個数はわからない)
            if (command instanceof Brew) {
                currentProductCount++;
                isLastSpurt = (upperProductCount - currentProductCount) == 1;
            }

            System.err.println(users[playerId].targetProduct);
            System.err.println(command.getClass());

            // in the first league: BREW <id> | WAIT; later: BREW <id> | CAST <id> [<times>] | LEARN <id> | REST | WAIT
            System.out.println(command.getCommandString());
        }
    }

}

class Product implements Comparable<Product> {
    int price;
    int actionId;
    String actionType;
    int delta0; //消費数。(元データはマイナスなので、プラスに変換して保持している)
    int delta1;
    int delta2;
    int delta3;
    double costPerformance;    //高いほど有益
    double priority;

    int staticStep;        //生成に必要な手数
    int step;              //現在の状態からな手数
    int[] spellCounts;       //必要なキャスト回数
    int restCount;        //必要な休憩回数

    public Product(int actionId, String actionType, int delta0, int delta1, int delta2, int delta3, int price, int tomeIndex, int taxCount, boolean castable, boolean repeatable) {
        this.actionId = actionId;
        this.actionType = actionType;
        this.delta0 = -delta0;
        this.delta1 = -delta1;
        this.delta2 = -delta2;
        this.delta3 = -delta3;
        this.price = price;
    }

    public void setPriority(boolean isLastSpurt, User me, User opponent) {
        //終盤は挙動を変える
        if (isLastSpurt) {
            if (opponent.score <= me.score + this.price) {
                this.priority = this.step; //すぐ作れるものを最優先
            } else {
                this.priority = Integer.MAX_VALUE;  //作っても負けるものは作らない
            }
            return;
        }

        this.costPerformance = ((double) price) / this.staticStep;
        this.priority = -this.costPerformance;  //コスパの高いものを優先
        System.err.print(this.priority + ",");
    }

    public boolean isCreatable(User user) {
        return (this.delta0 <= user.delta0)
                && (this.delta1 <= user.delta1)
                && (this.delta2 <= user.delta2)
                && (this.delta3 <= user.delta3);
    }

    @Override
    public int compareTo(Product o) {
        //優先度
        if (this.priority < o.priority) {
            return -1;
        } else if (this.priority > o.priority) {
            return 1;
        }

        //現状早く作れるものを優先
        if (this.step < o.step) {
            return -1;
        } else if (this.step > o.step) {
            return 1;
        }

        //高価なものをたくさん使うものは低評価
        if (this.delta3 < o.delta3) {
            return 1;
        } else if (this.delta2 < o.delta2) {
            return 1;
        } else if (this.delta1 < o.delta1) {
            return 1;
        } else if (this.delta0 < o.delta0) {
            return 1;
        }
        return 0;
    }

    /**
     * 指定したuserがプロダクトを作るための必要手数
     *
     * @param user
     */
    public void setStepToCreate(User user) {
        WayToMake way = this.getCreateWay(user);
        this.spellCounts = way.spellCounts;
        this.restCount = way.restCount;
        this.step = restCount + Arrays.stream(way.spellCounts).sum();

        //初めから作るとした場合のコストの算出
        User initialUser = new User(0, 0, 0, 0, 0);
        initialUser.setSpells(user.spells);
        WayToMake staticWay = this.getCreateWay(initialUser);
        this.staticStep = staticWay.restCount + Arrays.stream(staticWay.spellCounts).sum();
    }

    /**
     * 作るために必要な各作業のamount。
     * 各Spellが何から何を生むかを予め知っている。
     * @param user
     * @return
     */
    public WayToMake getCreateWay(User user) {
        int[] spellCounts = {0, 0, 0, 0};
        int[] restCounts = {0, 0, 0, 0};
        int[] requires = {
                this.delta0 - user.delta0,
                this.delta1 - user.delta1,
                this.delta2 - user.delta2,
                this.delta3 - user.delta3,
        };
        boolean[] spellCastable = {
                user.spells.get(0).castable,
                user.spells.get(1).castable,
                user.spells.get(2).castable,
                user.spells.get(3).castable,
        };

        while (spellCounts[3] < Math.max(0, requires[3])) {
            if (!spellCastable[3]) {
                restCounts[3] += 1;
                spellCastable[3] = true;
            }
            spellCounts[3] += 1;
            requires[2] += 1;
            requires[1] += 1;
            requires[0] += 1;
            spellCastable[3] = false;
        }

        while (spellCounts[2] < requires[2]) {
            if (!spellCastable[2]) {
                restCounts[2] += 1;
                spellCastable[2] = true;
            }
            spellCounts[2] += 1;
            requires[1] += 1;
            requires[0] += 1;
            spellCastable[2] = false;
        }

        while (spellCounts[1] < requires[1]) {
            if (!spellCastable[1]) {
                restCounts[1] += 1;
                spellCastable[1] = true;
            }
            spellCounts[1] += 1;
            requires[0] += 1;
            spellCastable[1] = false;
        }

        while ((spellCounts[0] * 2) < requires[0]) {
            if (!spellCastable[0]) {
                restCounts[0] += 1;
                spellCastable[0] = true;
            }
            spellCounts[0] += 1;
            spellCastable[0] = false;
        }

        int restCount = Math.max(Math.max(restCounts[0], restCounts[1]), Math.max(restCounts[2], restCounts[3]));
        return new WayToMake(spellCounts, restCount);
    }
}

class WayToMake {
    int spellCounts[];
    int restCount;

    public WayToMake(int[] spellCounts, int restCount) {
        this.spellCounts = spellCounts;
        this.restCount = restCount;
    }
}

class User implements Cloneable {
    int productCount = 0;
    int itemLimit = 10;
    int score;
    int delta0;
    int delta1;
    int delta2;
    int delta3;
    List<Spell> spells;
    Product targetProduct;

    public User(int delta0, int delta1, int delta2, int delta3, int score) {
        this.delta0 = delta0;
        this.delta1 = delta1;
        this.delta2 = delta2;
        this.delta3 = delta3;
        this.score = score;
    }

    public void setSpells(List<Spell> spells) {
        this.spells = spells;
    }

    public void setTargetProduct(Product product) {
        this.targetProduct = product;
    }

    public void addProductCount() {
        this.productCount++;
    }

    public Command getCommandToDo() throws NothingCanDoException {
        Product tp = this.targetProduct;
        System.err.println("target" + this.targetProduct.actionId);
        System.err.println(this.targetProduct.spellCounts[0]);
        System.err.println(this.targetProduct.spellCounts[1]);
        System.err.println(this.targetProduct.spellCounts[2]);
        System.err.println(this.targetProduct.spellCounts[3]);

        if (tp.isCreatable(this)) {
            System.err.println(this.targetProduct.actionId);
            this.addProductCount();
            return new Brew(this.targetProduct.actionId);
        }

        if (this.shouldDoSpell(this.targetProduct, 3)) {
            System.err.println(3);
            return this.spells.get(3);
        } else if (this.shouldDoSpell(this.targetProduct, 2)) {
            System.err.println(2);
            return this.spells.get(2);
        } else if (this.shouldDoSpell(this.targetProduct, 1)) {
            System.err.println(1);
            return this.spells.get(1);
        } else if (this.shouldDoSpell(this.targetProduct, 0) && !this.willPossessionOverflow(2)) {
            return this.spells.get(0);
        }

        if (this.spells.stream().allMatch(spell -> spell.castable)) {
            throw new NothingCanDoException();
        }
        System.err.println("REST");
        return new Rest();
    }

    private boolean shouldDoSpell(Product p, int spellId) {
        Spell spell = this.spells.get(spellId);
        return (0 < p.spellCounts[spellId]) //実行する必要がある
                && (spell.castable) //実行可能
                && (-spell.delta0 <= this.delta0)    //実行するだけの手持ちがある
                && (-spell.delta1 <= this.delta1)
                && (-spell.delta2 <= this.delta2)
                && (-spell.delta3 <= this.delta3);
    }

    private boolean willPossessionOverflow(int willGenerate) {
        return (this.delta0 + this.delta1 + this.delta2 + this.delta3 + willGenerate > itemLimit);
    }
}

class Spell extends Command {
    int actionId;
    int delta0;
    int delta1;
    int delta2;
    int delta3;
    boolean castable;

    Spell(int actionId, int delta0, int delta1, int delta2, int delta3, boolean castable) {
        this.actionId = actionId;
        this.delta0 = delta0;
        this.delta1 = delta1;
        this.delta2 = delta2;
        this.delta3 = delta3;
        this.castable = castable;
    }

    public String getCommandString() {
        return "CAST " + this.actionId;
    }
}

class Brew extends Command {
    public Brew(int actionId) {
        this.actionId = actionId;
    }

    public String getCommandString() {
        return "BREW " + this.actionId;
    }
}

class Rest extends Command {
    public String getCommandString() {
        return "REST";
    }
}

abstract class Command {
    int actionId;
    String actionType;

    abstract public String getCommandString();
}

@SuppressWarnings("serial")
class NothingCanDoException extends Exception {
}