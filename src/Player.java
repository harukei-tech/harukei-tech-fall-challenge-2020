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
        int opponentCount = 1;  //相手人数
        int upperProductCount = 6;
        int currentTurn = 0;
        int previousOpponentScore = 0;
        Me me = new Me(new int[]{0, 0, 0, 0}, 0);
        User opponent = new User(new int[]{0, 0, 0, 0}, 0);
        CreationMap myStaticCreationMap = null;

        User.productCountLimit = upperProductCount;

        //初めから作るとした場合のコストの算出
        User initialUser = new User(new int[]{0, 0, 0, 0}, 0);
        ArrayList<Spell> initialSpells = new ArrayList<>();
        initialSpells.add(new Spell(1, new int[]{2, 0, 0, 0}, true, false));
        initialSpells.add(new Spell(2, new int[]{-1, 1, 0, 0}, true, false));
        initialSpells.add(new Spell(3, new int[]{0, -1, 1, 0}, true, false));
        initialSpells.add(new Spell(4, new int[]{0, 0, -1, 1}, true, false));
        initialUser.setSpells(initialSpells);
        CreationMap initialStaticCreationMap = new CreationMap();
        initialStaticCreationMap.action(initialUser);

        // game loop
        while (true) {
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
                        Product p = new Product(actionId, actionType, ingredients, price, tomeIndex, taxCount, castable, repeatable);
                        p.setStaticStep(initialStaticCreationMap);
                        products.add(p);
                        break;
                    case "LEARN":
                        tome.add(new Spell(actionId, ingredients, castable, repeatable));
                        break;
                    default:
                        break;
                }
            }
            TimeKeeper.initTimeKeeper();

            User[] opponents = new User[opponentCount];

            for (int i = 0; i < 1 + opponentCount; i++) {
                int inv0 = in.nextInt(); // tier-0 ingredients in inventory
                int inv1 = in.nextInt();
                int inv2 = in.nextInt();
                int inv3 = in.nextInt();
                int score = in.nextInt(); // amount of rupees

                if (i == 0) {
                    me.update(new int[]{inv0, inv1, inv2, inv3}, score);
                    me.setSpells(mySpells);
                    me.setTome(tome);
                    me.setGameConditions(currentTurn);
                } else {
                    opponents[i - 1] = opponent;    //とりま相手は１人
                    opponents[i - 1].update(new int[]{inv0, inv1, inv2, inv3}, score);
                    opponents[i - 1].setSpells(opponentSpells);
                    if (score > previousOpponentScore) {
                        Logger.logln("add exec(op)");
                        opponents[i - 1].addProductCount();
                        previousOpponentScore = score;
                    }
                    opponents[i - 1].setGameConditions(currentTurn);
                }
            }

            //現在持っているSpellで一から作るとした場合のコストの算出
//            if (myStaticCreationMap == null) {
//                myStaticCreationMap = new CreationMap();
//                User initialUserWithMySpell = new User(new int[]{0, 0, 0, 0}, 0);
//                initialUserWithMySpell.setSpells(ListFactory.createSpellListCopy(me.spells));
//                myStaticCreationMap.action(initialUserWithMySpell);
//                me.setStaticCreationMap(myStaticCreationMap);
//            }

            me.setStrategyByCondition(opponents[0]);
            me.setRecipes(products);
            Logger.logln(me.strategy);

            //自分が作る場合の手順を作成
            if(me.strategy != Strategy.Start) {
                User simulationUser = new User(me.ingredients.clone(), 0);
                simulationUser.setSpells(ListFactory.createSpellListCopy(me.spells));
                CreationMap myCreationMap = new CreationMap();
                myCreationMap.action(simulationUser);
                me.setCreationMap(myCreationMap);
            }

            //作りたいものとプレイヤーの行動をセット
            Command command = me.getWhatWantToDo(opponents);

            //BREWする場合それを保持(ループで消えてしまうにも拘わらず入力データに無いための処置。相手の個数はわからない)
            if (command instanceof Brew) {
                Logger.logln("add exec");
                me.addProductCount();
                me.setTargetProduct(null);
                me.setStrategy(Strategy.CollectIngredient);
            } else if (command instanceof LEARN) {
                //手順の作り直し
                myStaticCreationMap = new CreationMap();
                User initialUserWithMySpell = new User(new int[]{0, 0, 0, 0}, 0);
                initialUserWithMySpell.setSpells(ListFactory.createSpellListCopy(me.spells));
                Logger.logln("my static creation map start");
                myStaticCreationMap.action(initialUserWithMySpell);
                Logger.logln("my static creation map end");
                me.setStaticCreationMap(myStaticCreationMap);
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
        for (boolean[][][] three : this.isFilled) {
            for (boolean[][] two : three) {
                for (boolean[] one : two) {
                    Arrays.fill(one, false);
                }
            }
        }

        for (boolean[][][] three : this.isRested) {
            for (boolean[][] two : three) {
                for (boolean[] one : two) {
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
        int previousStep = 0;
        long startTime = System.currentTimeMillis();
        while (!this.queue.isEmpty()) {
            count++;
            if (count % 200 == 0) {
                Logger.logln("count" + count);
            }
            if(System.currentTimeMillis() - startTime > 40) {
                Logger.logln("walk break");
                break;
            }
            SimulationUser u = this.queue.poll();
//            if(previousStep < u.step) {
//                Logger.logln(previousStep);
//                Logger.logln(u.step);
//                Logger.logln(this.queue.size());
//            }
            this.walk(u);
//            previousStep = u.step;
        }
        Logger.logln("walk end");

        Logger.logln("fill start");
        this.fillMap();
        Logger.logln("fill end");
    }

    private void walk(SimulationUser smuser) {
        if (this.maxLookahead < smuser.step) {
            return;
        }

        for (int i = 0; i < smuser.spells.size(); i++) {
            for (int time = 2; 1 <= time; time--) {  //重ね掛けと1回使用の2パターン
                if (smuser.canDoSpell(smuser.spells.get(i), time)) {
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

        if (!this.isRested[smuser.ingredients[0]][smuser.ingredients[1]][smuser.ingredients[2]][smuser.ingredients[3]]
                && smuser.restable() && smuser.step <= this.map.get(toKey(smuser.ingredients)).step) {
            SimulationUser u = new SimulationUser(smuser.ingredients.clone(), smuser.score);
            u.setSpells(ListFactory.createSpellListCopy(smuser.spells));
            u.rest();
            u.setBefore(smuser);
            u.setWhatDo(new Rest(), 1);
            u.step = smuser.step + 1;

            this.isRested[u.ingredients[0]][u.ingredients[1]][u.ingredients[2]][u.ingredients[3]] = true;

            this.queue.add(u);
        }
    }

    private void fillMap() {
        for (int firstIngredients = 10; 0 <= firstIngredients; firstIngredients--) {
            if (TimeKeeper.isTimeOver()) {
                Logger.logln("fill ended forthly");
                return;
            }
            for (int secondIngredients = (10 - firstIngredients); 0 <= secondIngredients; secondIngredients--) {
                for (int thirdIngredients = (10 - firstIngredients - secondIngredients); 0 <= thirdIngredients; thirdIngredients--) {
                    for (int forthIngredients = (10 - firstIngredients - secondIngredients - thirdIngredients); 0 <= forthIngredients; forthIngredients--) {
                        String key = CreationMap.toKey(new int[]{firstIngredients, secondIngredients, thirdIngredients, forthIngredients});
                        SimulationUser maxSmuser = this.map.get(key);

                        if (this.isFilled[firstIngredients][secondIngredients][thirdIngredients][forthIngredients]) {
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
                        if (TimeKeeper.isTimeOver()) {
                            Logger.logln("fillUnders ended forthly");
                            return;
                        }
                        if (i == firstIngredients && j == secondIngredients && k == thirdIngredients && l == forthIngredients) {
                            continue;
                        }
                        String key = CreationMap.toKey(new int[]{i, j, k, l});
                        SimulationUser smuser = this.map.get(key);
                        if (smuser == null || maxSmuser.step <= smuser.step) {
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
        return Integer.toString(ingredients[0] + ingredients[1]*100 + ingredients[2]*10000 + ingredients[3] * 1000000);
//        return Arrays.stream(ingredients).mapToObj(String::valueOf).collect(Collectors.joining("-")); //激重
    }
}

class Product implements Comparable<Product> {
    int price;
    int actionId;
    String actionType;
    int[] ingredients;//消費数。(元データはマイナスなので、プラスに変換して保持している)
    double costPerformance;    //高いほど有益
    double priority;

    int initialStaticStep;        //生成に必要な手数
    int step;              //現在の状態からな手数

    public Product(int actionId, String actionType, int[] ingredients, int price, int tomeIndex, int taxCount, boolean castable, boolean repeatable) {
        this.actionId = actionId;
        this.actionType = actionType;
        this.ingredients = Arrays.stream(ingredients).map(v->-v).toArray();
        this.price = price;
    }

    public void setPriority(Strategy strategy, Me me, User opponent) {
        //終盤は挙動を変える
        if (strategy == Strategy.LastSpurt) {
            this.priority = this.initialStaticStep; //コストの低いものを最優先
            return;
        } else if (strategy == Strategy.Last) {
            //作っても負けるものは作らない
            if ((opponent.score + opponent.extraScore) > (me.score + this.price)) {
                this.priority = Integer.MAX_VALUE;
            } else {
                this.priority = this.step; //すぐ作れるものを最優先
            }
            Logger.logln(this.actionId);
            Logger.logln(this.priority);
            return;
        }

        this.costPerformance = ((double) price) / this.initialStaticStep;
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
        this.initialStaticStep = staticMap.getStep(CreationMap.toKey(this.ingredients));
    }

    /**
     * プロダクトを作るための必要手数
     *
     * @param dynamicMap
     */
    public void setDynamicStep(CreationMap dynamicMap) {
        //初めから作るとした場合のコストの算出
        this.step = dynamicMap.getStep(CreationMap.toKey(this.ingredients));
    }
}

class Me extends User {
    Strategy strategy;
    Product targetProduct;          //作りたいもの
    CreationMap staticCreationMap;  //現在のSpellを持った人物で、所有数0からの手順
    CreationMap creationMap;    //現在の状態からの生成手順
    List<Product> recipes;

    int earlyTurn = 7;  //開始nターンを序盤と定義
    int lastSpurtRemainProductCount = 2;    //残りn個で終了、を終盤と定義

    public Me(int[] ingredients, int score) {
        super(ingredients, score);
        this.setStrategy(Strategy.Start);
    }

    public void setTargetProduct(Product product) {
        this.targetProduct = product;
    }

    public void setStrategy(Strategy strategy) {
        Logger.logln(strategy.name());
        this.strategy = strategy;
    }

    public void setRecipes(List<Product> recipes) {
        this.recipes = recipes;
    }

    public void setStrategyByCondition(User opponent) {
        if (this.currentTurn < this.earlyTurn) {
            this.setStrategy(Strategy.Start);
        } else if ((this.productCount + 1 == productCountLimit)
            || (opponent.productCount + 2 == productCountLimit //このターン1つ作ることでチェックがかかる場合
                 && this.recipes.stream().anyMatch(recipe->recipe.isCreatable(opponent))
                )
        ) {
            this.setStrategy(Strategy.Last);
        } else if (
                ((this.productCount + this.lastSpurtRemainProductCount >= productCountLimit)
                        || (opponent.productCount + this.lastSpurtRemainProductCount >= productCountLimit))
        && (this.productCount > 2)) {
            this.setStrategy(Strategy.LastSpurt);
        } else if (Strategy.CollectIngredient.compareTo(this.strategy) < 0) {
            this.setStrategy(Strategy.CreateProduct);
        } else {
            this.setStrategy(Strategy.CollectIngredient);
        }
    }

    public void setCreationMap(CreationMap creationMap) {
        this.creationMap = creationMap;
    }

    public void setStaticCreationMap(CreationMap staticCreationMap) {
        this.staticCreationMap = staticCreationMap;
    }

    public Command getWhatWantToDo(User[] opponents) {
        switch (this.strategy) {
            case Start:
                //始めは魔法の選択肢を広げる。効率は判定しない
                return new LEARN(this.tome.get(0).actionId);
            case CollectIngredient:
                if (this.targetProduct != null && this.targetProduct.isCreatable(this)) {
                    Logger.logln("goto creation");
                    this.setStrategy(Strategy.CreateProduct);
                    this.targetProduct = null;
                    return this.getWhatWantToDo(opponents);
                } else if (this.targetProduct == null) {
                    Logger.logln("start collecting");
                    this.setTargetProduct(this.getMostEfficientStatement());
                    return this.getWhatWantToDo(opponents);
                } else {
                    Logger.logln("continue creation");
                }
                break;
            case CreateProduct:
                Logger.logln("creating Product");
                Logger.logln(this.recipes.toString());
            case LastSpurt:
                Logger.logln("last spurt");
            case Last:
                //優先順位を設定
                for (Product product : this.recipes) {
                    product.setStaticStep(this.staticCreationMap);
                    product.setDynamicStep(this.creationMap);
                    product.setPriority(this.strategy, this, opponents[0]);
                }
                Collections.sort(this.recipes);
                for (Product p:this.recipes) {
                    if(this.creationMap.getStep(CreationMap.toKey(p.ingredients)) < Integer.MAX_VALUE) {
                        this.setTargetProduct(p);
                        break;
                    }
                }
                break;
        }

        try {
            return this.getCommandToDo();
        } catch (NothingCanDoException e) {
            //どうしようもなくなったら休憩またはスペル追加
            Logger.logln("Can Nothing");
//            if (this.restable()) {
                return new Rest();
//            } else {
//                return new LEARN(tome.get(0).actionId);
//            }
        }
    }

    public Command getCommandToDo() throws NothingCanDoException {
        Logger.logln("what want to create: " + Arrays.toString(this.targetProduct.ingredients));
        Product tp = this.targetProduct;
        Logger.logln("target" + this.targetProduct.actionId);

        if (tp.isCreatable(this)) {
            Logger.logln(Arrays.toString(tp.ingredients));
            Logger.logln(Arrays.toString(this.ingredients));
            Logger.logln(this.targetProduct.actionId);
            return new Brew(this.targetProduct.actionId);
        }

        String key = CreationMap.toKey(tp.ingredients);
        Logger.logln(key);
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
        } catch (NoSuchElementException e) {
            Logger.logln("myspells");
            this.spells.forEach(spell -> Logger.logln(spell.actionId));
            Logger.logln("wanted Command");
            Logger.logln(commandWantToDo.actionId);
            Logger.logln(commandWantToDo.getCommandString());
        }

        return commandWantToDo;
    }

    private Product getMostEfficientStatement() {
        long maxPerformance = 0;
        int[] maxConditions = new int[4];
        String key;
        int staticStep;
        int dynamicStep;

        for (int l = 10; 0 <= l; l--) {
            for (int k = 10-l; 0 <= k; k--) {
                for (int j = 10-l-k; 0 <= j; j--) {
                    for (int i = 10-l-k-j; 0 <= i; i--) {
                        key = CreationMap.toKey(new int[]{i, j, k, l});
                        staticStep = this.staticCreationMap.getStep(key);
                        dynamicStep = this.creationMap.getStep(key);
                        //staticStep上作れない扱いは排除
                        if ((staticStep - dynamicStep) >= maxPerformance && staticStep < 1000000) {
                            maxPerformance = staticStep - dynamicStep;
                            maxConditions = new int[]{-i, -j, -k, -l};
                            Logger.logln(staticStep);
                            Logger.logln(dynamicStep);
                            Logger.logln(Arrays.toString(maxConditions));
                        }
                    }
                }
            }
        }
        return new Product(0, "BREW", maxConditions, 0, 0, 0, false, false);
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
        if (time > 1) {
            ((Spell) this.whatDo).shouldRepeatNumber = time;
        }
    }

    public Command getWhatDoFirst() {
        SimulationUser user = this;
        while (user.before != null && user.before.whatDo != null) {
            Logger.logln(user.before.whatDo);
            user = user.before;
        }

        return user.whatDo;
    }
}

class User {
    static int productCountLimit;
    int currentTurn;
    int productCount;
    int itemLimit = 10;
    int score;
    int previousScore;
    int extraScore = 0;
    int[] ingredients;
    int ingredientSum;
    ArrayList<Spell> spells;
    List<Spell> tome;

    public User(int[] ingredients, int score) {
        this.ingredients = new int[4];
        this.ingredients = ingredients.clone();
        this.ingredientSum = Arrays.stream(ingredients).sum();  //streamは重いようなのであらかじめ計算しておく
        this.score = score;
        this.productCount = 0;
        this.extraScore = Arrays.stream(ingredients).sum() - this.ingredients[0];  //item0以外は得点として加算される
    }

    public void update(int[] ingredients, int score) {
        this.ingredients = ingredients;
        this.score = score;
    }

    public void setSpells(ArrayList<Spell> spells) {
        this.spells = spells;
    }

    public void setTome(List<Spell> spells) {
        this.tome = spells;
    }

    public void setGameConditions(int currentTurn) {
        this.currentTurn = currentTurn;
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
        return !this.willBagOverflow(spell.ingredientSum * times);
    }

    public void rest() {
        this.spells.forEach(spell -> spell.castable = true);
    }

    //休憩に意味があるか
    public boolean restable() {
        return this.spells.stream().anyMatch(spell -> !spell.castable);
    }
}

class Spell extends Command implements Cloneable {
    int[] ingredients;
    int ingredientSum;
    boolean castable;
    boolean repeatable;

    int shouldRepeatNumber;   //重ね掛けを実行するべきか(トレース結果の保持)

    Spell(int actionId, int[] ingredients, boolean castable, boolean repeatable) {
        this.actionId = actionId;
        this.ingredients = ingredients.clone();
        this.ingredientSum = Arrays.stream(ingredients).sum();
        this.castable = castable;
        this.repeatable = repeatable;
        this.shouldRepeatNumber = 1;
    }

    public String getCommandString() {
        return "CAST " + this.actionId + " " + this.shouldRepeatNumber;
    }

    @Override
    protected Spell clone() throws CloneNotSupportedException {
        return (Spell) super.clone();
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
    CollectIngredient,
    CreateProduct,
    LastSpurt,
    Last
}

class Logger {
    public static void log(Object s) {
//        System.err.print(Thread.currentThread().getStackTrace()[2].getLineNumber() + ":" + s + "::");
    }

    public static void logln(Object s) {
        System.err.println(Thread.currentThread().getStackTrace()[2].getLineNumber() + ":" + s + "::");
    }

//    public static void logln(Command s) {
//        System.err.println(Thread.currentThread().getStackTrace()[2].getLineNumber() + ":" + s + "::" + s.getCommandString());
//    }
}

class ListFactory {
    public static ArrayList<Spell> createSpellListCopy(ArrayList<Spell> spells) {
        ArrayList<Spell> newList = new ArrayList<>();
        spells.forEach(spell -> {
            try {
                newList.add((Spell) spell.clone());
            } catch (CloneNotSupportedException e) {
                e.printStackTrace();
            }
        });

        return newList;
    }
}

class TimeKeeper {
    static long startTime;

    static void initTimeKeeper (){
        startTime = System.currentTimeMillis();
    }

    static boolean isTimeOver () {
        return System.currentTimeMillis() - startTime > 40;
    }
}

@SuppressWarnings("serial")
class NothingCanDoException extends Exception {
}