import java.security.SecureRandom;
import java.util.*;

/**
 * gobang algorithm in core parts
 * @author pandora
 * @date 2019/1/14
 * @FixDate 2019/4/3
 * @Version 2.2
 */
public class WuZiChess {
    static class WuZi{//core object,abstract chess object
        int x;//location x
        int y;//location y
        int warn;//warn level,define dangerous level
        int except;//in except,under current path,the number of linked parts
        int level;//1->Horizontal -，2->Vertical |，3->Left /,4->Right \,0->init,default
        WuZi next;//under current path, the next one
        boolean visited;//in BFS,define search action or not
        boolean self;//player or ai(true or false)
        @Override
        public String toString() {
            return "WuZi{" +
                    "x=" + x +
                    ", y=" + y +
                    ", warn=" + warn +
                    ", except=" + except +
                    ", level=" + level +
                    ", visited=" + visited +
                    '}';
        }

        public WuZi(int x, int y, int warn, int except,boolean self) {
            this.x = x;
            this.y = y;
            this.warn = warn;
            this.except = except;
            this.self = self;
        }
    }
    static class XY{//define the search map key
        int x;//location x
        int y;//location y

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            XY xy = (XY) o;
            return x == xy.x &&
                    y == xy.y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }

        public XY(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
    Map<XY,WuZi> map;//save place the  location of WuZi
    Map<XY,WuZi> aiMap;//just save all ai location of ai
    Map<Integer,List<WuZi>> path;//save the any level path for player
    Map<Integer,List<WuZi>> aiPath;//save the any level path for ai
    Queue<WuZi> queue;//priority queue + BFS
    {
        map = new HashMap<>();
        aiMap = new HashMap<>();
        queue = new ArrayDeque<>();
        path = new HashMap();
        aiPath = new HashMap<>();
    }

    /**
     * Just for player BFS
     * @param moveX
     * @param moveY
     * @return
     */
    WuZi BFS(int moveX,int moveY){
        if(moveX<0||moveX>15||moveY<0||moveY>15) throw new RuntimeException("x，y has some value are exception");
        XY xy = new XY(moveX,moveY);
        for (WuZi temp:map.values()) {//reset the status
            if(temp.visited) temp.visited = false;
        }
        path.clear();
        queue.add(map.get(xy));
        WuZi node;
        while(!queue.isEmpty()){
            node = queue.poll();
            node.visited = true;
            for(int x=-1;x<=1;x++){
                for(int y=-1;y<=1;y++){
                    if(x==0&&y==0)continue;//myself -> skip
                    WuZi wuZi = map.get(new XY(node.x+x,y+node.y));
                    if(wuZi!=null){
                        /**
                         * search the relative position
                         * ===start=====
                         */
                        if(node.self == wuZi.self) {
                            if (y == 0) node.level = 1;
                            if (x == 0) node.level = 2;
                            if (x + y == 0) node.level = 4;
                            if (Math.abs(x + y) == 2) node.level = 3;
//                        if (Math.abs(wuZi.x - node.x) == 1 && wuZi.y - node.y == 0) node.level = wuZi.level = 1;
//                        if (Math.abs(wuZi.y - node.y) == 1 && wuZi.x - node.x == 0) node.level = wuZi.level = 2;
                            if (wuZi.next != null && wuZi.next.level != wuZi.level)
                                wuZi.except = 1;//if not linked,set the oridinal value
                            /**
                             * ===end====
                             */
                            if (wuZi.level == node.level && wuZi.next != node) {
                                List<WuZi> list = null;
                                if(node.self && wuZi.self) list = path.get(wuZi.level);
                                if (list == null) list = new ArrayList<>();
                                if (!list.contains(node)) list.add(node);
                                if (!list.contains(wuZi)) list.add(wuZi);
                                if(node.self) path.put(node.level, list);
                                if(node.self) {
                                    node.except = wuZi.except + 1;
                                    if (node.except > node.warn) {
                                        node.warn = node.except;
                                    }
                                    node.next = wuZi;
                                }
                            }
                        }
//                        if(node.warn==5) return node;
                        if(path.get(node.level)!=null&&path.get(node.level).size()==5&&node.self) return node;//found it for player!
                        if(!wuZi.visited && wuZi.self) queue.add(wuZi);
                    }
                }
            }
        }
        return null;//found failed
    }

    /**
     * Just for AI BFS
     * @param moveX
     * @param moveY
     * @return
     */
    WuZi BFSForAI(int moveX,int moveY){
        if(moveX<0||moveX>15||moveY<0||moveY>15) throw new RuntimeException("x，y has some value are exception");
        XY xy = new XY(moveX,moveY);
        for (WuZi temp:aiMap.values()) {//reset the status
            if(temp.visited) temp.visited = false;
        }
        aiPath.clear();
        queue.add(aiMap.get(xy));
        WuZi node;
        while(!queue.isEmpty()){
            node = queue.poll();
            node.visited = true;
            for(int x=-1;x<=1;x++){
                for(int y=-1;y<=1;y++){
                    if(x==0&&y==0)continue;//myself -> skip
                    WuZi wuZi = aiMap.get(new XY(node.x+x,y+node.y));
                    if(wuZi!=null){
                        /**
                         * search the relative position
                         * ===start=====
                         */
                        if (y == 0) node.level = 1;
                        if (x == 0) node.level = 2;
                        if (x + y == 0) node.level = 4;
                        if (Math.abs(x + y) == 2) node.level = 3;
                        /**
                         * ===end====
                         */
                        if (wuZi.level == node.level) {
                            List<WuZi> list = aiPath.get(wuZi.level);
                            if (list == null) list = new ArrayList<>();
                            if (!list.contains(node)) list.add(node);
                            if (!list.contains(wuZi)) list.add(wuZi);
                            path.put(node.level, list);
                        }
                        if(aiPath.get(node.level)!=null&&aiPath.get(node.level).size()==5) return node;//found it for player!
                        if(!wuZi.visited) queue.add(wuZi);
                    }
                }
            }
        }
        return null;//found failed
    }
    static boolean location[][] = new boolean[16][16];//mark the location
    /**
     * test by some virtual data
     * model type:ai first,then you
     * @param args
     */
    public static void main(String[] args) {
        WuZiChess wuZiChess = new WuZiChess();
        Scanner scanner = new Scanner(System.in);
        WuZi result = null;
        for(int i=0;i<10;i++) {
            System.out.println("input x：");
            int x=scanner.nextInt();
            System.out.println("input y：");
            int y=scanner.nextInt();
            XY xy = new XY(x, y);
            if(wuZiChess.map.get(xy)!=null) throw new RuntimeException("current position has a chess model,can't reset");
            WuZi wuZi = new WuZi(x,y,1,1,true);
            wuZiChess.map.put(xy,wuZi);
            location[x][y]= true;
            result = wuZiChess.BFS(xy.x,xy.y);
            WuZi ai =  wuZiChess.find(xy, wuZiChess);
            if(ai!=null){
                XY aiXY = new XY(ai.x,ai.y);
                wuZiChess.map.put(aiXY,ai);
                wuZiChess.aiMap.put(aiXY,ai);
                location[ai.x][ai.y]=true;
                System.out.println("ai auto model:["+ai.x+","+ai.y+"]");
            }
            if(result == null) result = wuZiChess.BFSForAI(ai.x,ai.y);
            if(result!=null) break;
    }
//    while(result!=null){
//            System.out.println(result);
//            result = result.next;
//    }
        if(result!=null&&result.self) System.out.println("your winner:"+ wuZiChess.path.values());//every level path,includes one to five for player
        if(result!=null&&!result.self) System.out.println("your loser:"+ wuZiChess.aiPath.values());//every level path,includes one to five for ai
    }

    private WuZi find(XY defaultPos, WuZiChess wuZiChess) {
        WuZi returnPos = wuZiChess.findRandom(defaultPos, wuZiChess);//return predicted position
        if(returnPos==null) return null;
        if(wuZiChess.map.size()==0|| wuZiChess.path.size()==0){
            return returnPos;//empty? directly return a random position
        }
        List<WuZi> maxLength = null;//search the loggest path
        WuZi predict = null;//search auto suitable location
        XY xy = new XY(-1,-1);
        WuZi temp = null;
        for (List<WuZi> obj: wuZiChess.path.values()) if(maxLength==null||obj.size()>maxLength.size()) maxLength = obj;//find the biggest
        for (WuZi danger:maxLength) if(predict==null||danger.warn >= predict.warn) predict = danger;//find the most dangerous location
            for (;;)
                if(predict!=null) {
                    switch (predict.level) {
                        case 1:// -
                            if (predict.x - 1 >= 0) {
                                xy.x=predict.x - 1;
                                xy.y=predict.y;
                                temp = wuZiChess.map.get(xy);
                            }
                            if (temp != null || xy.x<0) {
                                if (predict.x + 1 < 15) {
                                    xy.x = predict.x + 1;
                                    xy.y = predict.y;
                                    temp = wuZiChess.map.get(xy);
                                }
                            }
                            if (temp == null) {
                                returnPos.x = xy.x;
                                returnPos.y = xy.y;
                                return returnPos;
                            } else predict = predict.next;
                            break;
                        case 2:// |
                            if (predict.y - 1 >= 0) {
                                xy.x = predict.x;
                                xy.y = predict.y - 1;
                                temp = wuZiChess.map.get(xy);
                            }
                            if (temp != null ||xy.y<0) {
                                if (predict.y + 1 < 15) {
                                    xy.x = predict.x;
                                    xy.y = predict.y + 1;
                                    temp = wuZiChess.map.get(xy);
                                }
                            }
                            if (temp == null) {
                                returnPos.x = xy.x;
                                returnPos.y = xy.y;
                                return returnPos;
                            } else predict = predict.next;
                            break;
                        case 3:// /
                            if (predict.x - 1 >= 0 && predict.y - 1 >= 0) {
                                xy.x=predict.x - 1;
                                xy.y=predict.y - 1;
                                temp = wuZiChess.map.get(xy);
                            }
                            if (temp != null || xy.x<0||xy.y<0) {
                                if (predict.x + 1 < 15 || predict.y + 1 < 15) {
                                    xy.x=predict.x + 1;
                                    xy.y=predict.y + 1;
                                    temp = wuZiChess.map.get(xy);
                                }
                            }
                            if (temp == null) {
                                returnPos.x = xy.x;
                                returnPos.y = xy.y;
                                return returnPos;
                            } else predict = predict.next;
                            break;
                        case 4:// \
                            if (predict.x - 1 >= 0 || predict.y + 1 < 15) {
                                xy.x=predict.x - 1;
                                xy.y=predict.y + 1;
                                temp = wuZiChess.map.get(xy);
                            }
                            if (temp != null || xy.x<0) {
                                if (predict.x + 1 < 15 || predict.y - 1 >= 0) {
                                    xy.x=predict.x + 1;
                                    xy.y=predict.y - 1;
                                    temp = wuZiChess.map.get(xy);
                                }
                            }
                            if (temp == null) {
                                returnPos.x = xy.x;
                                returnPos.y = xy.y;
                                return returnPos;
                            } else predict = predict.next;
                            break;
                        default:
                            throw new RuntimeException("parameter is incorrect!");
                    }
                }
                else return returnPos;
    }
    int ratio = 2;//record the random ratio,it should become big when stack search count increase.Just defending waiting util time out!
    int counter;//record that what time the ratio will become increase
    SecureRandom secureRandom = new SecureRandom();//the way that (int)Math.random()*ratio is not enough secure for RNG

    /**
     * @fixDate 2019/3/23
     * @message refuse to use a deeep stack search by a while
     * @param defaultPos
     * @param wuZiChess
     * @return
     */
    private  WuZi findRandom(XY defaultPos, WuZiChess wuZiChess){//find a random position
//        int x = defaultPos.x+secureRandom.nextInt(ratio)-1;
//        int y = defaultPos.y+secureRandom.nextInt(ratio)-1;
//        WuZi temp = wuZiChess.map.get(new XY(x,y));
//        while(temp!=null||x<0||y<0||x>15||y>15) {
//            if(counter > 5){
//                ratio = ratio+1>15?15:ratio+1;
//                counter = 0;
//            }
//            counter++;
//            x = defaultPos.x+secureRandom.nextInt(ratio)-1;
//            y = defaultPos.y+secureRandom.nextInt(ratio)-1;
//        }
//        ratio = 2;//reset
//        counter = 0;//reset
        int x = 0;
        int y = 0;
        boolean flag = false;
        back:
        for(;x<location.length;x++)
            for(;y<location[0].length;y++)
                if (!location[x][y]) {
                    flag = true;
                    break back;
                }
        return flag?new WuZi(x, y, 0, 0, false):null;
    }
}
