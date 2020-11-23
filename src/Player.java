import java.util.*;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Auto-generated code below aims at helping you parse
 * the standard input according to the problem statement.
 **/
class Player {
    public static void main(String args[]) {
        Scanner in = new Scanner(System.in);
        int opponentCount = 1;  //相手人数
        int upperProductCount = 6;
        int currentProductCount = 0;
        int currentTurn = 0;

        User.productCountLimit = upperProductCount;

        // game loop
        while (true) {
            long time = System.currentTimeMillis();
            currentTurn++;

            int actionCount = in.nextInt(); // the number of spells and recipes in play
            List<Product> products = new ArrayList<>();

            //javaではオブジェクトの配列が作れないらしい
            ArrayList<Spell> mySpells = new ArrayList<>();
            ArrayList<Spell> opponentSpells = new ArrayList<>(); //相手は1人なので配列にしていない
            List<Spell> tome = new ArrayList<>();   //先読みの有用性がわからないので作りこんでいない

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
                int[] ingredients = new int[]{delta0, delta1, delta2, delta3};
                switch (actionType) {
                    case "CAST":
                        mySpells.add(new Spell(actionId, ingredients, castable, repeatable));
                        break;
                    case "OPPONENT_CAST":
                        opponentSpells.add(new Spell(actionId, ingredients, castable, repeatable));
                        break;
                    case "BREW":
                        products.add(new Product(actionId, actionType, ingredients, price, tomeIndex, taxCount, castable, repeatable));
                        break;
                    case "LEARN":
                        tome.add(new Spell(actionId, ingredients, castable, repeatable));
                        break;
                    default:
                        break;
                }
            }

            Me me = null;
            User[] opponents = new User[opponentCount];

            for (int i = 0; i < 1 + opponentCount; i++) {
                int inv0 = in.nextInt(); // tier-0 ingredients in inventory
                int inv1 = in.nextInt();
                int inv2 = in.nextInt();
                int inv3 = in.nextInt();
                int score = in.nextInt(); // amount of rupees

                if (i == 0) {
                    me = new Me(new int[]{inv0, inv1, inv2, inv3}, score);
                    me.setSpells(mySpells);
                    me.setTome(tome);
                    me.setGameConditions(currentTurn, currentProductCount);
                    me.setStrategy();
                } else {
                    opponents[i - 1] = new User(new int[]{inv0, inv1, inv2, inv3}, score);
                    opponents[i - 1].setSpells(opponentSpells);
                }
            }

            //初めから作るとした場合のコストの算出
            User initialUser = new User(new int[]{0, 0, 0, 0}, 0);
            initialUser.setSpells(ListFactory.createSpellListCopy(me.spells));
            CreationMap staticCreationMap = new CreationMap();

            staticCreationMap.action(initialUser);

            //優先順位を設定
            for (Product product : products) {
                product.setStaticStep(staticCreationMap);
                product.setPriority(me.strategy, me, opponents[0]);
            }

            Collections.sort(products);

            //自分が作る場合の手順を作成
            User simulationUser = new User(me.ingredients.clone(),0);
            simulationUser.setSpells(ListFactory.createSpellListCopy(me.spells));
            CreationMap myCreationMap = new CreationMap();
            Logger.logln("how many time" + (System.currentTimeMillis() - time));
            myCreationMap.action(simulationUser);
            me.setCreationMap(myCreationMap);
            Logger.logln(myCreationMap.map);

            //作りたいものとプレイヤーの行動をセット
            Command command = null;
            for (Product product : products) {
                me.setTargetProduct(product);
                try {
                    command = me.getCommandToDo();
                    break;
                } catch (NothingCanDoException e) {
                    //何もできず詰んだ場合は作るプロダクトを変える
                    continue;
                }
            }
            //どうしようもなくなったらスペルを追加
            if (command == null) {
                Logger.logln("Can Nothing");
//                command = new LEARN(tome.get(0).actionId);
                command = new Rest();
            }

            //BREWする場合それを保持(ループで消えてしまうにも拘わらず入力データに無いための処置。相手の個数はわからない)
            if (command instanceof Brew) {
                currentProductCount++;
            }

            Logger.logln(me.targetProduct);
            Logger.logln(command.getClass());

            // in the first league: BREW <id> | WAIT; later: BREW <id> | CAST <id> [<times>] | LEARN <id> | REST | WAIT
            System.out.println(command.getCommandString());
        }
    }

}

class CreationMap {
    Map<String, SimulationUser> map;
    Queue<SimulationUser> queue;
    int maxLookahead = 10;
    boolean[][][][] isFilled;
    boolean[][][][] isRested;
    long startTime;

    CreationMap() {
        this.map = new HashMap<>();
        this.queue = new PriorityQueue<SimulationUser>((u, o) -> Integer.compare(u.step, o.step));
        this.isFilled = new boolean[11][11][11][11];    //所持個数は0～10の11パターン
        this.isRested = new boolean[11][11][11][11];    //所持個数は0～10の11パターン
        for(boolean[][][] three:this.isFilled) {
            for(boolean[][] two:three) {
                for(boolean[] one:two) {
                    Arrays.fill(one, false);
                }
            }
        }

        for(boolean[][][] three:this.isRested) {
            for(boolean[][] two:three) {
                for(boolean[] one:two) {
                    Arrays.fill(one, false);
                }
            }
        }
    }

    public int getStep(String key) {
        if (this.map.get(key) == null) {
            return Integer.MAX_VALUE;
        }
        return this.map.get(key).step;
    }

    public void action(User user) {
        this.startTime = System.currentTimeMillis();
        Logger.logln(this.startTime);
        SimulationUser smuser = new SimulationUser(user.ingredients.clone(), user.score);
        smuser.setSpells(ListFactory.createSpellListCopy(user.spells));
        smuser.step = 0;
        this.map.put(CreationMap.toKey(smuser.ingredients), smuser);
        this.queue.add(smuser);
        Logger.logln("walk start");
        int count = 0;
        while (!this.queue.isEmpty()) {
            count++;
            if(count % 200 == 0) {
                Logger.logln("count" + count);
            }
            if((System.currentTimeMillis() - this.startTime) > 20) {
                Logger.logln("action break");
                Logger.logln(System.currentTimeMillis() - this.startTime);
                break;
            }
            SimulationUser u = this.queue.poll();
            this.walk(u);
        }
        Logger.logln(System.currentTimeMillis() - this.startTime);
        Logger.logln("walk end");

        Logger.logln("fill start");
        this.fillMap();
        Logger.logln(System.currentTimeMillis() - this.startTime);
        Logger.logln("fill end");
    }

    private void walk(SimulationUser smuser) {
        if (this.maxLookahead < smuser.step) {
            return;
        }

        if((System.currentTimeMillis() - this.startTime) > 20) {
            Logger.logln(smuser.before.whatDo.getClass() + ",");
            Logger.logln(System.currentTimeMillis() - this.startTime);
            return;
        }

        for(int i=0; i<smuser.spells.size(); i++) {
            for(int time = 2; 1 <= time; time--) {  //重ね掛けと1回使用の2パターン
                if(smuser.canDoSpell(smuser.spells.get(i), time)) {
                    SimulationUser u = new SimulationUser(smuser.ingredients.clone(), smuser.score);
                    u.setSpells(ListFactory.createSpellListCopy(smuser.spells));
                    u.useSpell(u.spells.get(i), time);
                    if (!this.map.containsKey(toKey(u.ingredients))) {
                        u.setBefore(smuser);
                        u.setWhatDo(u.spells.get(i), time);
                        u.step = smuser.step + 1;
                        this.map.put(toKey(u.ingredients), u);
                        this.queue.add(u);
                    }
                }
            }
        }

        String key = toKey(smuser.ingredients);
        if (!this.isRested[smuser.ingredients[0]][smuser.ingredients[1]][smuser.ingredients[2]][smuser.ingredients[3]]
                && smuser.restable() && smuser.step <= this.map.get(key).step) {
            SimulationUser u = new SimulationUser(smuser.ingredients.clone(), smuser.score);
            u.setSpells(ListFactory.createSpellListCopy(smuser.spells));
            u.rest();
            u.setBefore(smuser);
            u.setWhatDo(new Rest(), 1);
            u.step = smuser.step + 1;

            this.isRested[u.ingredients[0]][u.ingredients[1]][u.ingredients[2]][u.ingredients[3]]=true;

            this.queue.add(u);
        }
    }

    private void fillMap() {
        for (int firstIngredients = 10; 0 <= firstIngredients; firstIngredients--) {
            for (int secondIngredients = (10 - firstIngredients); 0 <= secondIngredients; secondIngredients--) {
                for (int thirdIngredients = (10 - firstIngredients - secondIngredients); 0 <= thirdIngredients; thirdIngredients--) {
                    for (int forthIngredients = (10 - firstIngredients - secondIngredients - thirdIngredients); 0 <= forthIngredients; forthIngredients--) {
                        if((System.currentTimeMillis() - this.startTime) > 20) {
                            Logger.logln("fill ended forthly");
                            Logger.logln(System.currentTimeMillis() - startTime);
                            return;
                        }
                        String key = CreationMap.toKey(new int[]{firstIngredients, secondIngredients, thirdIngredients, forthIngredients});
                        SimulationUser maxSmuser = this.map.get(key);

                        if(this.isFilled[firstIngredients][secondIngredients][thirdIngredients][forthIngredients]) {
                            break;
                        } else if (maxSmuser != null) {
                            this.fillUnders(firstIngredients, secondIngredients, thirdIngredients, forthIngredients, maxSmuser);
                        }
                    }
                }
            }
        }
    }

    /**
     * 引数無しfillMapから呼ばれる
     *
     * @param firstIngredients
     * @param secondIngredients
     * @param thirdIngredients
     * @param forthIngredients
     * @param maxSmuser
     */
    private void fillUnders(int firstIngredients, int secondIngredients, int thirdIngredients, int forthIngredients, SimulationUser maxSmuser) {
        for (int i = firstIngredients; 0 <= i; i--) {
            for (int j = secondIngredients; 0 <= j; j--) {
                for (int k = thirdIngredients; 0 <= k; k--) {
                    for (int l = forthIngredients; 0 <= l; l--) {
                        if(System.currentTimeMillis() - startTime > 20) {
                            Logger.logln("fillUnders ended forthly");
                            Logger.logln(System.currentTimeMillis() - startTime);
                            return;
                        }
                        if(i==firstIngredients && j==secondIngredients && k==thirdIngredients && l==forthIngredients) {
                            continue;
                        }
                        String key = CreationMap.toKey(new int[]{i, j, k, l});
                        SimulationUser smuser = this.map.get(key);
                        if (smuser == null || maxSmuser.step < smuser.step) {
                            this.map.put(key, maxSmuser);
                            this.isFilled[i][j][k][l] = true;
                        } else if (maxSmuser.step >= smuser.step) {
                            break;
                        }
                    }
                }
            }
        }
    }

    /**
     * 配列の先頭から、数字をハイフンでつなげた文字列を返却
     *
     * @param ingredients
     * @return
     */
    public static String toKey(int[] ingredients) {
        return Arrays.stream(ingredients).mapToObj(String::valueOf).collect(Collectors.joining("-"));
    }
}

class Product implements Comparable<Product> {
    int price;
    int actionId;
    String actionType;
    int[] ingredients;//消費数。(元データはマイナスなので、プラスに変換して保持している)
    double costPerformance;    //高いほど有益
    double priority;

    int staticStep;        //生成に必要な手数
    int step;              //現在の状態からな手数

    public Product(int actionId, String actionType, int[] ingredients, int price, int tomeIndex, int taxCount, boolean castable, boolean repeatable) {
        this.actionId = actionId;
        this.actionType = actionType;
        this.ingredients = Arrays.stream(ingredients).map(v -> -v).toArray();
        this.price = price;
    }

    public void setPriority(Strategy strategy, User me, User opponent) {
        //終盤は挙動を変える
        if (strategy == Strategy.LastSpurt) {
            if ((opponent.score + opponent.extraScore) <= (me.score + this.price)) {
                this.priority = this.step; //すぐ作れるものを最優先
            } else {
                this.priority = Integer.MAX_VALUE;  //作っても負けるものは作らない
            }
            return;
        }

        this.costPerformance = ((double) price) / this.staticStep;
        this.priority = -this.costPerformance;  //コスパの高いものを優先
        Logger.log(this.priority + ",");
    }

    public boolean isCreatable(User user) {
        //1つでもアイテムが足りないならfalse
        for (int i = 0; i < user.ingredients.length; i++) {
            if (user.ingredients[i] < this.ingredients[i]) {
                return false;
            }
        }
        return true;
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
        for (int i = 3; i >= 0; i--) {
            if (this.ingredients[i] < o.ingredients[i]) {
                return 1;
            }
        }
        return 0;
    }

    /**
     * プロダクトを作るための必要手数
     *
     * @param staticMap
     */
    public void setStaticStep(CreationMap staticMap) {
        //初めから作るとした場合のコストの算出
        this.staticStep = staticMap.getStep(CreationMap.toKey(this.ingredients));
    }
}

class Me extends User {
    Strategy strategy;
    int currentTurn;
    Product targetProduct;
    CreationMap creationMap;

    int earlyTurn = 7;  //開始nターンを序盤と定義
    int lastSpurtRemainProductCount = 2;    //残りn個で終了、を終盤と定義

    public Me(int[] ingredients, int score) {
        super(ingredients, score);
    }

    public void setTargetProduct(Product product) {
        this.targetProduct = product;
    }

    public void setGameConditions(int currentTurn, int currentProductCount) {
        this.currentTurn = currentTurn;
        this.productCount = currentProductCount;
    }

    public void setStrategy() {
        if (this.currentTurn < this.earlyTurn) {
            this.strategy = Strategy.Start;
        } else if (this.productCount + this.lastSpurtRemainProductCount == productCountLimit) {
            this.strategy = Strategy.LastSpurt;
        } else {
            this.strategy = Strategy.Basic;
        }
    }

    public void setCreationMap(CreationMap creationMap) {
        this.creationMap = creationMap;
    }

    public Command getCommandToDo() throws NothingCanDoException {
        //始めは魔法の選択肢を広げる。効率は判定しない
        if (this.strategy == Strategy.Start) {
            return new LEARN(this.tome.get(0).actionId);
        }

        Product tp = this.targetProduct;
        Logger.logln("target" + this.targetProduct.actionId);

        if (tp.isCreatable(this)) {
            Logger.logln(this.targetProduct.actionId);
            this.addProductCount();
            return new Brew(this.targetProduct.actionId);
        }

        String key = CreationMap.toKey(tp.ingredients);
//        Logger.logln(key);
        if (this.creationMap.map.get(key) == null) {
//            Logger.logln(this.creationMap.map.toString());
            throw new NothingCanDoException();
        }

        Command commandWantToDo = this.creationMap.map.get(key).getWhatDoFirst();
        try {
            if (commandWantToDo instanceof Spell && !this.spells.stream().filter(spell -> spell.actionId == commandWantToDo.actionId).findFirst().get().castable) {
                Logger.logln("have to rest");
                return new Rest();
            }
        } catch(NoSuchElementException e) {
            Logger.logln("myspells");
            this.spells.forEach(spell -> Logger.logln(spell.actionId));
            Logger.logln("wanted Command");
            Logger.logln(commandWantToDo.actionId);
            Logger.logln(commandWantToDo.getCommandString());
        }

        return commandWantToDo;
    }
}

class SimulationUser extends User {
    int step = 0;
    SimulationUser before;
    Command whatDo;

    public SimulationUser(int[] ingredients, int score) {
        super(ingredients, score);
    }

    public void setBefore(SimulationUser before) {
        this.before = before;
    }

    public void setWhatDo(Command whatDo, int time) {
        this.whatDo = whatDo;
        if(time > 1) {
            ((Spell) this.whatDo).shouldRepeatNumber = time;
        }
    }

    public Command getWhatDoFirst() {
        SimulationUser user = this;
        while (user.before != null && user.before.whatDo != null) {
            user = user.before;
        }

        return user.whatDo;
    }
}

class User {
    static int productCountLimit;
    int productCount = 0;
    int itemLimit = 10;
    int score;
    int extraScore = 0;
    int[] ingredients;
    ArrayList<Spell> spells;
    List<Spell> tome;

    public User(int[] ingredients, int score) {
        this.ingredients = new int[4];
        this.ingredients = ingredients.clone();
        this.score = score;
        this.extraScore = Arrays.stream(this.ingredients).sum() - this.ingredients[0];  //item0以外は得点として加算される
    }

    public void setSpells(ArrayList<Spell> spells) {
        this.spells = spells;
    }

    public void setTome(List<Spell> spells) {
        this.tome = spells;
    }

    public void addProductCount() {
        this.productCount++;
    }

    protected boolean willBagOverflow(int willGenerate) {
        return (Arrays.stream(this.ingredients).sum() + willGenerate) > this.itemLimit;
    }

    public void useSpell(Spell spell, int times) {
        for (int i = 0; i < this.ingredients.length; i++) {
            this.ingredients[i] += spell.ingredients[i] * times;
        }
        spell.castable = false;
    }

    protected boolean canDoSpell(Spell spell, int times) {
        if (!spell.castable) {
            return false;   //実行不可違反
        }

        if (!spell.repeatable && 1 < times) {
            return false;   //複数使えない制限違反
        }

        //消費個数を所有しているか
        for (int i = 0; i < this.ingredients.length; i++) {
            //消費する分が足りているか、なのでspell結果のマイナス値
            if (this.ingredients[i] < -spell.ingredients[i] * times) {
                return false;
            }
        }

        //所持上限違反
        return !this.willBagOverflow(Arrays.stream(spell.ingredients).sum() * times);
    }

    public void rest() {
        this.spells.forEach(spell -> spell.castable = true);
    }

    //休憩に意味があるか
    public boolean restable() {
        return this.spells.stream().anyMatch(spell -> !spell.castable);
    }
}

class Spell extends Command implements Cloneable{
    int[] ingredients;
    boolean castable;
    boolean repeatable;

    int shouldRepeatNumber;   //重ね掛けを実行するべきか(トレース結果の保持)

    Spell(int actionId, int[] ingredients, boolean castable, boolean repeatable) {
        this.actionId = actionId;
        this.ingredients = ingredients.clone();
        this.castable = castable;
        this.repeatable = repeatable;
        this.shouldRepeatNumber = 1;
    }

    public String getCommandString() {
        return "CAST " + this.actionId + " " + this.shouldRepeatNumber;
    }

    @Override
    protected Spell clone() throws CloneNotSupportedException {
        return (Spell)super.clone();
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

class LEARN extends Command {
    public LEARN(int actionId) {
        this.actionId = actionId;
    }

    public String getCommandString() {
        return "LEARN " + this.actionId;
    }
}

abstract class Command {
    int actionId;
    String actionType;

    abstract public String getCommandString();
}

enum Strategy {
    Start,
    Basic,
    LastSpurt
}

class Logger{
    public static void log(Object s) {
//        System.err.print(Thread.currentThread().getStackTrace()[2].getLineNumber() + ":" + s + "::");
    }

    public static void logln(Object s) {
//        System.err.println(Thread.currentThread().getStackTrace()[2].getLineNumber() + ":" + s + "::");
    }

//    public static void logln(Command s) {
//        System.err.println(Thread.currentThread().getStackTrace()[2].getLineNumber() + ":" + s + "::" + s.getCommandString());
//    }
}

class ListFactory {
    public static ArrayList<Spell> createSpellListCopy(ArrayList<Spell> spells)
    {
        ArrayList<Spell> newList = new ArrayList<>();
        spells.forEach(spell -> {
            try {
                newList.add((Spell)spell.clone());
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        });

        return newList;
    }
}

@SuppressWarnings("serial")
class NothingCanDoException extends Exception {
}