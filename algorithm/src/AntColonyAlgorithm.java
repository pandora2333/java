import org.junit.jupiter.api.Test;
/**
 *  Ant colony algorithm
 *  LB model
 *  @author by pandora
 * @date 2019/1/16
 */
public class AntColonyAlgorithm {
    /** 任务集合(tasks[i]表示第i个任务的长度) */
    int[] tasks;
    // 任务数量
    int taskNum = 100;

    /** 处理节点集合(nodes[i]表示第i个处理节点的处理速度) */
    int[] nodes;
    // 处理节点数量
    int nodeNum = 10;

    /** 迭代次数 */
    int iteratorNum = 100;

    /** 蚂蚁的数量 */
    int antNum = 100;

    /** 任务处理时间矩阵(记录单个任务在不同节点上的处理时间) */
    float[][] timeMatrix;

    /** 信息素矩阵(记录每条路径上当前信息素含量，初始状态下均为0.0f) */
    float[][] pheromoneMatrix;

    /** 最大信息素的下标矩阵(存储当前信息素矩阵中每行最大信息素的下标) */
    int[] maxPheromoneMatrix;

    /** 一次迭代中，随机分配的蚂蚁临界编号(该临界点之前的蚂蚁采用最大信息素下标，而该临界点之后的蚂蚁采用随机分配) */
    int[] criticalPointMatrix;

    /** 任务处理时间结果集([迭代次数][蚂蚁编号]) */
    float[][] resultData;

    /** 每次迭代信息素衰减的比例 */
    float p = 0.5f;

    /** 每次经过，信息素增加的比例 */
    float q = 2f;

    /**
     * 入口函数
     */
    @Test
    public void init(){

        // 初始化任务集合+初始化节点集合
        initRandomArray(taskNum,nodeNum);

        // 执行蚁群算法
        aca();

        //输出每次迭代的resultData
        for (int iter = 0;iter<resultData.length;iter++) {
            System.out.println("iter: 第"+(iter+1)+" 次");
            for (float result : resultData[iter])
                System.out.print(result + " ");
            System.out.println();
        }
    }

    /**
     * 蚁群算法
     */
    private void aca() {
        // 初始化任务执行时间矩阵
        initTimeMatrix(tasks, nodes);

        // 初始化信息素矩阵
        initPheromoneMatrix(taskNum, nodeNum);

        // 迭代搜索
        acaSearch(taskNum,nodeNum,iteratorNum, antNum);
    }

    /**
     * 迭代搜索
     * @param iteratorNum 迭代次数
     * @param antNum 蚂蚁数量
     * @param taskNum 任务总数
     * @param nodeNum 节点数
     */
    private void acaSearch(int taskNum,int nodeNum,int iteratorNum, int antNum) {
        maxPheromoneMatrix = new int[taskNum];
        criticalPointMatrix = new int[antNum];
        resultData = new float[iteratorNum][antNum];
        for (int itCount = 0; itCount < iteratorNum; itCount++) {
            // 本次迭代中，所有蚂蚁的路径
            Object[] pathMatrix_allAnt = new Object[antNum];
            for (int antCount = 0; antCount < antNum; antCount++) {
                // 第antCount只蚂蚁的分配策略(pathMatrix[i][j]表示第antCount只蚂蚁将i任务分配给j节点处理)
                int[][] pathMatrix_oneAnt = new int[taskNum][nodeNum];
                for (int taskCount = 0; taskCount < taskNum; taskCount++) {
                    // 将第taskCount个任务分配给第nodeCount个节点处理
                    int nodeCount = assignOneTask(antCount, taskCount);
                    pathMatrix_oneAnt[taskCount][nodeCount] = 1;
                }
                // 将当前蚂蚁的路径加入pathMatrix_allAnt
                pathMatrix_allAnt[antCount] = pathMatrix_oneAnt;
            }

            // 计算 本次迭代中 所有蚂蚁 的任务处理时间
            float[] timeArray_oneIt = calTime_oneIt(pathMatrix_allAnt);
            // 将本地迭代中 所有蚂蚁的 任务处理时间加入总结果集
            resultData[itCount] = timeArray_oneIt;

            // 更新信息素
            updatePheromoneMatrix(pathMatrix_allAnt,timeArray_oneIt);

        }
    }

    /**
     * 更新信息素
     * @param pathMatrix_allAnt 本次迭代中所有蚂蚁的行走路径
     * @param timeArray_oneIt 本次迭代的任务处理时间的结果集
     */
    private void updatePheromoneMatrix(Object[] pathMatrix_allAnt,float[] timeArray_oneIt) {
        // 所有信息素均衰减p%
        for (int i=0; i<taskNum; i++) {
            for (int j=0; j<nodeNum; j++) {
                pheromoneMatrix[i][j] *= p;
            }
        }

        // 找出任务处理时间最短的蚂蚁编号
        float minTime = Float.MAX_VALUE;
        int minIndex = -1;
        for (int antIndex=0; antIndex<antNum; antIndex++) {
            if (timeArray_oneIt[antIndex] < minTime) {
                minTime = timeArray_oneIt[antIndex];
                minIndex = antIndex;
            }
        }

        // 将本次迭代中最优路径的信息素增加到q%
        for (int taskIndex=0; taskIndex<taskNum; taskIndex++) {
            for (int nodeIndex=0; nodeIndex<nodeNum; nodeIndex++) {
                if (((int[][])pathMatrix_allAnt[minIndex])[taskIndex][nodeIndex] == 1) {
                    pheromoneMatrix[taskIndex][nodeIndex] *= q;
                }
            }
        }
        for (int taskIndex=0; taskIndex<taskNum; taskIndex++) {
            float maxPheromone = pheromoneMatrix[taskIndex][0];
            int maxIndex = 0;
            float sumPheromone = pheromoneMatrix[taskIndex][0];
            boolean isAllSame = true;

            for (int nodeIndex=1; nodeIndex<nodeNum; nodeIndex++) {
                if (pheromoneMatrix[taskIndex][nodeIndex] > maxPheromone) {
                    maxPheromone = pheromoneMatrix[taskIndex][nodeIndex];
                    maxIndex = nodeIndex;
                }

                if (pheromoneMatrix[taskIndex][nodeIndex] != pheromoneMatrix[taskIndex][nodeIndex-1]){
                    isAllSame = false;
                }

                sumPheromone += pheromoneMatrix[taskIndex][nodeIndex];
            }

            // 若本行信息素全都相等，则随机选择一个作为最大信息素
            if (isAllSame==true) {
                maxIndex = (int)Math.random()* (nodeNum-1);
                maxPheromone = pheromoneMatrix[taskIndex][maxIndex];
            }

            // 将本行最大信息素的下标加入maxPheromoneMatrix
            maxPheromoneMatrix[taskIndex] = maxIndex;

            // 将本次迭代的蚂蚁临界编号加入criticalPointMatrix(该临界点之前的蚂蚁的任务分配根据最大信息素原则，而该临界点之后的蚂蚁采用随机分配策略)
            criticalPointMatrix[taskIndex] = Math.round(antNum * (maxPheromone/sumPheromone));
        }
    }

    /**
     * 计算一次迭代中，所有蚂蚁的任务处理时间
     * @param pathMatrix_allAnt 所有蚂蚁的路径
     */
    private float[] calTime_oneIt(Object[] pathMatrix_allAnt) {

        float[] time_allAnt = new float[pathMatrix_allAnt.length];
        for (int antIndex=0; antIndex<pathMatrix_allAnt.length; antIndex++) {
            // 获取第antIndex只蚂蚁的行走路径
             int[][] pathMatrix = (int[][]) pathMatrix_allAnt[antIndex];

            // 获取处理时间最长的节点 对应的处理时间
            float maxTime = -1;
            for (int nodeIndex=0; nodeIndex<nodeNum; nodeIndex++) {
                // 计算节点taskIndex的任务处理时间
                float time = 0;
                for (int taskIndex=0; taskIndex<taskNum; taskIndex++) {
                    if (pathMatrix[taskIndex][nodeIndex] == 1) {
                        time += timeMatrix[taskIndex][nodeIndex];
                    }
                }
                // 更新maxTime
                if (time > maxTime) {
                    maxTime = time;
                }
            }

            time_allAnt[antIndex] = maxTime;
        }
        return time_allAnt;
    }

    /**
     * 将第taskCount个任务分配给某一个节点处理
     * @param antCount 蚂蚁编号
     * @param taskCount 任务编号
     */
    private int assignOneTask(int antCount, int taskCount) {
        // 若当前蚂蚁编号在临界点之前，则采用最大信息素的分配方式
        if (antCount <= criticalPointMatrix[taskCount]) {
            return maxPheromoneMatrix[taskCount];
        }

        // 若当前蚂蚁编号在临界点之后，则采用随机分配方式
        return (int) (Math.random()*(nodeNum-1));
    }

    /**
     * 初始化信息素矩阵(全为0)
     * @param taskNum 任务数量
     * @param nodeNum 节点数量
     */
    private void initPheromoneMatrix(int taskNum, int nodeNum) {
        pheromoneMatrix = new float[taskNum][nodeNum];
        for(int i = 0;i<taskNum;i++)
            for (int j = 0;j<nodeNum;j++)
                pheromoneMatrix[i][j] = 1f;
    }

    /**
     * 初始化时间矩阵
     * @param tasks
     * @param nodes
     */
    private void initTimeMatrix(int[] tasks, int[] nodes) {

        for(int i = 0;i<tasks.length;i++)
            for (int j = 0;j<nodes.length;j++)
                timeMatrix[i][j] = tasks[i]*.1f/nodes[j];

    }

    /**
     * 初始化任务长度，节点处理速度
     * @param taskNum
     * @param nodeNum
     */
    private void initRandomArray(int taskNum,int nodeNum) {
        tasks = new int[taskNum];
        nodes = new int[nodeNum];
        timeMatrix = new float[taskNum][nodeNum];
        for(int task = 0;task<tasks.length;task++) tasks[task] = (int)(Math.random()*91+10);//random task length [10.100]
        for (int node = 0;node<nodes.length;node++) nodes[node] = (int)(Math.random()*91+10);//random node handle speed  [10.100]
    }
}
