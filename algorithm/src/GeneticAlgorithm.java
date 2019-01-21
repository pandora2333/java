import org.junit.jupiter.api.Test;

/**
 * Genetic Algorithm
 *LB model
 * @author by pandora
 * @date 2019/1/16
 */
public class GeneticAlgorithm {
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

    /** 染色体数量 */
    int chromosomeNum = 10;

    /** 任务处理时间矩阵(记录单个任务在不同节点上的处理时间) */
    float[][] timeMatrix;

    /** 适应度矩阵(下标：染色体编号、值：该染色体的适应度) */
    float[] adaptability;
    
    /** 自然选择的概率矩阵(下标：染色体编号、值：该染色体被选择的概率) */
    float[] selectionProbability;

    /** 染色体复制的比例(每代中保留适应度较高的染色体直接成为下一代) */
    float cp = 0.2f;//[0,1]
    
    /** 参与交叉变异的染色体数量 */
    int crossoverMutationNum;

    /** 任务处理时间结果集([迭代次数][染色体编号]) */
    float[][] resultData;


    /**
     * 入口函数
     */
    @Test
    public void init(){

        // 初始化任务集合+初始化节点集合
        initRandomArray(taskNum,nodeNum);

        // 执行遗传算法
        ga();

        //输出每次迭代的resultData
        for (int iter = 0;iter<resultData.length;iter++) {
            System.out.println("iter: 第"+(iter+1)+" 次");
            for (float result : resultData[iter])
                System.out.print(result + " ");
            System.out.println();
        }
    }

    /**
     * 遗传算法
     */
    private void ga() {
        // 初始化任务执行时间矩阵
        initTimeMatrix(tasks, nodes);

        // 迭代搜索
        gaSearch(iteratorNum, chromosomeNum);
    }

    /**
     * 迭代搜索
     * @param iteratorNum 迭代次数
     * @param chromosomeNum 染色体数量
     */
    private void gaSearch(int iteratorNum, int chromosomeNum) {

        // 初始化第一代染色体
        int[][] chromosomeMatrix = createGeneration(null,0);

        // 迭代繁衍
        for (int itIndex=1; itIndex<iteratorNum; itIndex++) {
            // 计算上一代各条染色体的适应度
            calAdaptability(chromosomeMatrix);

            // 计算自然选择概率
            calSelectionProbability(adaptability);

            // 生成新一代染色体
            chromosomeMatrix = createGeneration(chromosomeMatrix,itIndex);

        }
    }

    /**
     * 计算自然选择概率
     * @param adaptability
     */
    private void calSelectionProbability(float[] adaptability) {
        selectionProbability = new float[chromosomeNum];
        // 计算适应度总和
        float sumAdaptability = 0;
        for (int i=0; i<chromosomeNum; i++) {
            sumAdaptability += adaptability[i];
        }

        // 计算每条染色体的选择概率
        for (int i=0; i<chromosomeNum; i++) {
            selectionProbability[i] = adaptability[i] / sumAdaptability;
        }
    }


    /**
     * 计算 染色体适应度
     * @param chromosomeMatrix
     */
    private void calAdaptability(int[][] chromosomeMatrix) {
        adaptability = new float[chromosomeNum];
        // 计算每条染色体的任务长度
        for (int chromosomeIndex=0; chromosomeIndex<chromosomeNum; chromosomeIndex++) {
            float maxLength = Float.MIN_VALUE;
            for (int nodeIndex=0; nodeIndex<nodeNum; nodeIndex++) {
                float sumLength = 0;
                for (int taskIndex=0; taskIndex<taskNum; taskIndex++) {
                    if (chromosomeMatrix[chromosomeIndex][taskIndex] == nodeIndex) {
                        sumLength += timeMatrix[taskIndex][nodeIndex];
                    }
                }

                if (sumLength > maxLength) {
                    maxLength = sumLength;
                }
            }

            // 适应度 = 1/任务长度
            adaptability[chromosomeIndex] = 1/maxLength;
        }
    }

    /**
     * 繁衍新一代染色体
     * @param chromosomeMatrix 上一代染色体
     * @param itIndex 迭代次数
     */
    private int[][] createGeneration(int[][] chromosomeMatrix,int itIndex) {
        // 第一代染色体，随机生成
        if (chromosomeMatrix == null) {
            if(resultData==null) resultData = new float[iteratorNum][chromosomeNum];//init resultData array
            int[][] newChromosomeMatrix = new int[chromosomeNum][taskNum];
            for (int chromosomeIndex=0; chromosomeIndex<chromosomeNum; chromosomeIndex++)
                for (int taskIndex=0; taskIndex<taskNum; taskIndex++) 
                    newChromosomeMatrix[chromosomeIndex][taskIndex] = (int) (Math.random()*(nodeNum-1));

            // 计算当前染色体的任务处理时间
            calTime_oneIt(newChromosomeMatrix,itIndex);
            return newChromosomeMatrix;
        }

        // 交叉生成{crossoverMutationNum}条染色体
        int[][] newChromosomeMatrix = cross(chromosomeMatrix);

        // 变异
        newChromosomeMatrix = mutation(newChromosomeMatrix);

        // 复制
        newChromosomeMatrix = copy(chromosomeMatrix, newChromosomeMatrix);

        // 计算当前染色体的任务处理时间
        calTime_oneIt(newChromosomeMatrix,itIndex);

        return newChromosomeMatrix;
    }

    /**
     * 复制(复制上一代中优良的染色体)
     * @param chromosomeMatrix 上一代染色体矩阵
     * @param newChromosomeMatrix 新一代染色体矩阵
     */
    private int[][] copy(int[][] chromosomeMatrix, int[][] newChromosomeMatrix) {
        // 寻找适应度最高的N条染色体的下标(N=染色体数量*复制比例)
        int[] chromosomeIndexArr = maxN(adaptability, (int)(chromosomeNum*cp+1));

        // 复制
        for (int i=0; i<chromosomeIndexArr.length; i++) {
            int[] chromosome = chromosomeMatrix[chromosomeIndexArr[i]];
            newChromosomeMatrix[i+crossoverMutationNum] = chromosome;
        }

        return newChromosomeMatrix; 
    }

    /**
     * 从数组中寻找最大的n个元素
     * @param adaptability
     * @param n
     */
    private int[] maxN(float[] adaptability, int n) {
        // 将一切数组升级成二维数组，二维数组的每一行都有两个元素构成[原一位数组的下标,值]
        float[][] matrix = new float[adaptability.length][2];
        for (int i=0; i<adaptability.length; i++) {
            matrix[i][0] = i;
            matrix[i][1] = adaptability[i];
        }

        // 对二维数组排序（传统冒泡）
        for (int i=0; i<matrix.length-1; i++) {
            boolean flag = true;
            for (int j=0; j<matrix.length-i-1; j++) {
                if (matrix[j+1][1] < matrix[j][1]) {
                    float[] temp = matrix[j+1];
                    matrix[j+1] = matrix[j];
                    matrix[j] = temp;
                    flag = false;
                }
            }
            if(flag) break;
        }

        // 取最大的n个元素
        int[] maxIndexArray = new int[n];
        for (int i=matrix.length-1,id=0; i>matrix.length-n-1; i--,id++) {
            maxIndexArray[id] = (int) matrix[i][0];
        }

        return maxIndexArray;
    }

    /**
     * 变异
     * @param newChromosomeMatrix 新一代染色体矩阵
     */
    private int[][] mutation(int[][] newChromosomeMatrix) {

        // 随机找一条染色体
        int chromosomeIndex = (int) (Math.random()*(crossoverMutationNum-1));

        // 随机找一个任务
        int taskIndex = (int) (Math.random()*(taskNum-1));

        // 随机找一个节点
        int nodeIndex = (int) (Math.random()*(nodeNum-1));

        newChromosomeMatrix[chromosomeIndex][taskIndex] = nodeIndex;

        return newChromosomeMatrix;
    }

    /**
     * 交叉生成{crossoverMutationNum}条染色体
     * @param chromosomeMatrix 上一代染色体矩阵
     */
    private int[][] cross(int[][] chromosomeMatrix) {

        int[][] newChromosomeMatrix = new int[chromosomeNum][taskNum];
        for (int chromosomeIndex = 0; chromosomeIndex<crossoverMutationNum; chromosomeIndex++) {

            // 采用轮盘赌选择父母染色体
            int[] chromosomeBaba = chromosomeMatrix[RWS(selectionProbability)];
            int[] chromosomeMama = chromosomeMatrix[RWS(selectionProbability)];
            // 交叉
            int crossIndex = (int)(Math.random()*(taskNum-1));
            for(int baba = crossIndex+1,mama = 0 ;baba<taskNum;baba++,mama++)
                chromosomeBaba[baba] = chromosomeMama[mama];
            newChromosomeMatrix[chromosomeIndex] = chromosomeBaba;
        }
        return newChromosomeMatrix;
    }

    /**
     * 轮盘赌算法
     * @param selectionProbability 概率数组(下标：元素编号、值：该元素对应的概率)
     * @returns {number} 返回概率数组中某一元素的下标
     */
    private int RWS(float[] selectionProbability) {
        int sum = 0;
        float rand = (float) Math.random();
        for (int i=0; i<selectionProbability.length; i++) {
            sum += selectionProbability[i];
            if (sum >= rand) {
                return i;
            }
        }
        return (int) (Math.random()*(chromosomeNum-1));//found not ,just random any one
    }

    /**
     * 计算所有染色体的任务处理时间
     * @param chromosomeMatrix
     */
    private void calTime_oneIt(int[][] chromosomeMatrix,int iteratorNum) {

        // 计算每条染色体的任务长度
        float[] timeArray_oneIt = new float[chromosomeNum];
        for (int chromosomeIndex=0; chromosomeIndex<chromosomeNum; chromosomeIndex++) {
            float maxLength = Float.MIN_VALUE;
            for (int nodeIndex=0; nodeIndex<nodeNum; nodeIndex++) {
                float sumLength = 0;
                for (int taskIndex=0; taskIndex<taskNum; taskIndex++) {
                    if (chromosomeMatrix[chromosomeIndex][taskIndex] == nodeIndex) {
                        sumLength += timeMatrix[taskIndex][nodeIndex];
                    }
                }

                if (sumLength > maxLength) {
                    maxLength = sumLength;
                }
            }

            timeArray_oneIt[chromosomeIndex] = maxLength;
        }
        resultData[iteratorNum] = timeArray_oneIt;
    }

    /**
     * 初始化任务长度，节点处理速度
     * @param taskNum
     * @param nodeNum
     */
    private void initRandomArray(int taskNum,int nodeNum) {
        crossoverMutationNum = (int) (chromosomeNum - 1- chromosomeNum*cp);//init cross mutation
        tasks = new int[taskNum];
        nodes = new int[nodeNum];
        timeMatrix = new float[taskNum][nodeNum];
        for(int task = 0;task<tasks.length;task++) tasks[task] = (int)(Math.random()*91+10);//random task length [10.100]
        for (int node = 0;node<nodes.length;node++) nodes[node] = (int)(Math.random()*91+10);//random node handle speed  [10.100]
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
}
