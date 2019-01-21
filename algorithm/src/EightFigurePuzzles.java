
import org.junit.platform.commons.util.StringUtils;

import java.util.*;
/**
 * AStar Algorithm resolve Eight-figure Puzzles
 * @author pandora
 * @date 2019/1/19
 * @Version 2.0
 */
public class EightFigurePuzzles {

    /**
     * adstract location model
     */
    static class Status{
        int x,y;
        char value;//save some char
    }

    /**
     * abstarct access path model
     */
    static class Path{
        String val;
        Path pre;
        int f,g,h;
        public Path(String val, Path pre) {
            this.val = val;
            this.pre = pre;
        }
    }

    /**
     * abstract F value,as openList key
     */
    static class Int{
        int f;
        int id;
        @Override
        public boolean equals(Object o) {//save many possibility ,the same as f value
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Int anInt = (Int) o;
            return f == anInt.f &&
                    id == anInt.id;
        }
        @Override
        public int hashCode() {
            return Objects.hash(f, id);
        }

        public Int(int f, int id) {
            this.f = f;
            this.id = id;
        }
    }

    /**
     * search as the map key
     */
    static  class XY{
        int x,y;
        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            XY xy = (XY) o;
            return x == xy.x &&
                    y == xy.y;
        }

        public XY(int x, int y) {
            this.x = x;
            this.y = y;
        }

        @Override
        public int hashCode() {
            return Objects.hash(x, y);
        }
    }

    /**
     * calc the H value
     * @param origin
     * @param dest
     * @return
     */
    public int H(Status[] origin,Status[] dest){
        int h = 0;
        if(origin.length!=dest.length&&origin.length!=9) throw  new RuntimeException("incorrent pattern in H");
        for (int i = 0 ;i<origin.length;i++)
            if(origin[i].value!=dest[i].value)
                h++;

        return h;
    }

    /**
     * calc the G value
     * @param current
     * @return
     */
    public int G(Path current) {
        return current == null?0:current.g + 1;
    }

    /**
     * is dest arrays ?
     * @param current
     * @return
     */
    public boolean success(Path current){
        if(current == null) return false;
        if(current.h == 0) return true;
        else return false;
    }

    Map<XY,Status> map = new HashMap<>();//save all position,conveniently find it！
    /**
     * find from origin arrays to the dest arrays by AStar Algorithm
     * @param origin
     * @param dest
     * @param blank
     * @return
     */
    public Path Astar(Status[] origin,Status[] dest,char blank){
        Map<Int,Path> openList = new HashMap<>();//opneList ,use key find value
        Map<String,Int> forFVal = new HashMap<>();//use value find key
        Queue<String> closedList = new ArrayDeque<>();//closedList
        Path startPath = new Path(str(origin),null);//init beginning path
        startPath.h = H(origin,dest);
        startPath.g = G(null);
        startPath.f = startPath.g + startPath.h;
        Int preInt = new Int(startPath.f,0);//as previous f value that need to remove
        if(startPath.h  == 0) return startPath;//origin is the same as dest,directly return dest array
        openList.put(preInt,startPath);//save it in openList
        Path pre = startPath;//previous path,mapped by next path
        Path next;//next path
        int id = 0;//as auto increment for F value as an Int Object
        int[] dx = {-1,0,1,0};//face the arrays in any row: up(+) and down(-)
        int[] dy = {0,1,0,-1};//face the arrays in  any column: left(-) and right(+)
        while(!openList.isEmpty()){//when openList is empty,all paths has been searching
            Status blk = null;//record the blank position,use it move
            for (int i = 0; i < origin.length; i++)//find the blank position
                if (origin[i].value == blank) {
                    blk = origin[i];
                    break;
                }
            if(blk == null||blk.x <0 || blk.y<0) throw  new RuntimeException("incorrect pattern in AStar,no separator!");
            XY xy;//move the blk's x and y;
            for (int index = 0;index<dx.length;index++){
                xy = new XY(blk.x+dx[index],blk.y+dy[index]);
                Status tmp = map.get(xy);
                if(tmp!=null){//maybe over  max X or max Y
                    id++;
                    tmp.x = blk.x;//firstly,swap tmp and blk position to calc H,G,F
                    tmp.y = blk.y;
                    blk.x = xy .x;
                    blk.y = xy.y;
                    String str = str(origin);
                    next = new Path(str,pre);
                    next.pre = pre;
                    next.h = H(origin,dest);
                    next.g = G(pre);//find previous XY's G value,and calc it
                    next.f = next.h + next.g;
                    if(success(next)) {
                        System.out.println("at least steps:"+pre.g);
                        return pre;//if finding it,game over
                    }
                    if(!closedList.contains(str)) {
                        Int f = new Int(next.f,id);
                        if (forFVal.get(str)!=null&&forFVal.get(str).f > next.f) {
                            Int removeKey = forFVal.get(str);
                            forFVal.put(str,f);//update
                            openList.remove(removeKey);
                            openList.put(f,next);//min F update value,new one
                        }else{
                            openList.put(f,next);//save one status path
                            forFVal.put(str,f);
                        }
                    }
                    blk.y = tmp.y;//swap again,get previous position for blk
                    blk.x = tmp.x;
                    tmp.x = xy.x;
                    tmp.y = xy.y;
                }
            }

            openList.remove(preInt);//remove original path,find next new suitable F value
            closedList.add(pre.val);//save it in closeList
            forFVal.remove(pre.val);//remove it from the collection of F value
            Int removeKey = null;//key ,will not use it
            for(Int index:openList.keySet())//find the lowest F value
                if(removeKey == null||removeKey.f > index.f) removeKey = index;
            Path lowerF = openList.get(removeKey);//find the lowest Staus String
            if(lowerF!=null) {
                pre = lowerF;//swap pre
                preInt = removeKey;//swap preInt
                origin = parseStr(lowerF.val,true);//replace pre path
            }
        }
        return  null;//found not ,just fail
    }

    /**
     * Status[] -> String
     * @param origin
     * @return
     */
    private String str(Status[] origin) {
        StringBuffer sb = new StringBuffer();
        Object[][] tmp = new Object[3][3];//one V up two V array,show current all values\
        if(origin == null) throw  new RuntimeException("status is null");
        for (Status status:origin)
            if(status!=null&&status.x>=0&&status.y>=0&&status.x<3&&status.y<3) tmp[status.x][status.y] = status.value;
            else throw new RuntimeException("incorrect x ,y");
        for (int i = 0;i<tmp.length;i++)
            for (int j = 0; j < tmp[i].length; j++)
                sb.append(String.valueOf(tmp[i][j]));
        return sb.toString();
    }


    int iter;//record the iteractor count

    /**
     * print current status String
     * for example
     * 123456780
     * ->
     * 1 2 3
     * 4 5 6
     * 7 8 0
     * @param statuse
     */
    public void print(String statuse){
        Object[][] tmp = new Object[3][3];//one V up two V array,show current all values
        if(statuse == null|| StringUtils.isBlank(statuse)) throw  new RuntimeException("status is null");
        Status[] statuses = parseStr(statuse,false);
        for (Status status:statuses)
            if(status!=null&&status.x>=0&&status.y>=0&&status.x<3&&status.y<3) tmp[status.x][status.y] = status.value;
            else throw new RuntimeException("incorrect x ,y");
        System.out.println("current "+iter+":");
        for (int i = 0;i<tmp.length;i++) {
            for (int j = 0; j < tmp[i].length; j++) {
                System.out.print(tmp[i][j] + " ");
            }
            System.out.println();
        }
        iter++;
    }

    /**
     * String -> Status[]
     * @param statuse
     * @param flag
     * @return
     */
    private Status[] parseStr(String statuse,boolean flag) {
        Status[] statuses = new Status[9];
        String[] originStr = statuse.split("");
        int index = 0;
        if(flag) this.map.clear();
        for (int i =0;i<3;i++)
            for(int j = 0;j<3;j++,index++) {
                statuses[index] = new Status();
                statuses[index].x = i;
                statuses[index].y  = j;
                if(flag) this.map.put(new XY(i, j), statuses[index]);
                statuses[index].value = originStr[index].charAt(0);
            }
        return  statuses;
    }

    /**
     * have any resolution?
     * @param origin
     * @param dest
     * @return
     */
    public Boolean isCansolve(Status[] origin,Status[] dest) {
        int i ,j;
        int resultOfStart=0;
        int resultOfTarget = 0;
        for(i=0;i<9;i++)
        {
            for(j=0;j<i;j++)
            {
                if(origin[j].value < origin[i].value && origin[j].value!='0')
                    resultOfStart++;
                if(dest[j].value < dest[i].value&&dest[j].value!='0')
                    resultOfTarget++;
            }
        }
        return (resultOfStart & 1) == (resultOfTarget & 1);
    }
    @org.junit.jupiter.api.Test
    public void init(){//can not use Scanner to input data
    }

    /**
     * some data test
     * @param args
     */
    public static void main(String[] args) {
        //init some data
        Status[] origin = new Status[9];
        Status[] dest = new Status[9];
        EightFigurePuzzles eightFigurePuzzles = new EightFigurePuzzles();
        String[] originStr = "014276385".trim().split("");//014276385  283104765
        String[] destStr = "123456780".trim().split("");//123456780  123804765
        int index = 0;
        for (int i =0;i<3;i++)
            for(int j = 0;j<3;j++,index++) {
                origin[index] = new Status();
                dest[index] = new Status();
                origin[index].x = dest[index].x = i;
                origin[index].y = dest[index].y = j;
                origin[index].value = originStr[index].charAt(0);
                eightFigurePuzzles.map.put(new XY(i,j),origin[index]);
                dest[index].value = destStr[index].charAt(0);
            }
        //init end
        if(!eightFigurePuzzles.isCansolve(origin,dest)) throw new RuntimeException("it will no resolution");
        long firstTime = System.currentTimeMillis();
        Path path = eightFigurePuzzles.Astar(origin,dest,'0');
        long endTime = System.currentTimeMillis();
        System.out.println("spend time:"+(endTime-firstTime)+"ms");
        if(path == null) throw  new RuntimeException("no result");
        Stack<String> resovlePath = new Stack<>(); //save correct chain for paths
        while(path!=null) {
            resovlePath.push(path.val);
            path = path.pre;
        }
        while(!resovlePath.isEmpty())
            eightFigurePuzzles.print(resovlePath.pop());//show all paths
    }
}
