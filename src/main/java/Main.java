package main.java;

import java.util.Arrays;
import java.util.Collections;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.RecursiveTask;

/*
                Параллельное умножение матриц методом Штрассена Fork-Join-Pool
*/
public class Main extends RecursiveTask<int[][]>
{
    int n;
    int[][] a;
    int[][] b;

    public Main(int[][] a, int[][] b, int n)
    {
        this.a = a;
        this.b = b;
        this.n = n;
    }

    public static void main(String[] args)
    {
        int n = 10;
        int m = 10;
        System.out.println("Matrix A");
        int[][] a = randMat(n, m);
        printM(a);
        System.out.println();
        System.out.println("Matrix B");
        int[][] b = randMat(n, m);
        printM(b);
        System.out.println();

        System.out.println("Result");
        int[][] matrix = mulFJ(a, b);
        printM(matrix);

    }

    public static int[][] randMat(int n, int m)
    {
        int[][] a = new int[n][m];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < m; j++)
                a[i][j] = new Random().nextInt(10);
        return a;
    }
    private static void printM(int[][] a)
    {
        for (int i = 0; i < a.length; i++){
            for ( int j = 0; j < a[0].length; j++)
                System.out.print(a[i][j] + " ");
            System.out.println();
        }
    }
    public static int[][] mulFJ(int[][] a, int[][] b)
    {
        int n = getDim(a, b);
        int[][] a1 = addition2SquareMatrix(a, n);
        int[][] b1 = addition2SquareMatrix(b, n);

        Main main = new Main(a1, b1, n);
        ForkJoinPool pool = new ForkJoinPool();
        int[][] fj = pool.invoke(main);

        return getSubM(fj, a.length, b[0].length);
    }

    @Override
    protected int[][] compute()
    {
        if (n <= 4) return mul(a, b);
        n = n >> 1;

        int[][] a11 = new int[n][n];
        int[][] a12 = new int[n][n];
        int[][] a21 = new int[n][n];
        int[][] a22 = new int[n][n];

        int[][] b11 = new int[n][n];
        int[][] b12 = new int[n][n];
        int[][] b21 = new int[n][n];
        int[][] b22 = new int[n][n];

        split(a, a11, a12, a21, a22);
        split(b, b11, b12, b21, b22);

        Main mp1 = new Main(sum(a11, a22), sum(b11, b22), n);
        Main mp2 = new Main(sum(a21, a22), b11, n);
        Main mp3 = new Main(a11, sub(b12, b22), n);
        Main mp4 = new Main(a22, sub(b21, b11), n);
        Main mp5 = new Main(sum(a11, a12), b22, n);
        Main mp6 = new Main(sub(a21, a11), sum(b11, b12), n);
        Main mp7 = new Main(sub(a12, a22), sum(b21, b22), n);

        mp1.fork();
        mp2.fork();
        mp3.fork();
        mp4.fork();
        mp5.fork();
        mp6.fork();
        mp7.fork();

        int[][] p1 = mp1.join();
        int[][] p2 = mp2.join();
        int[][] p3 = mp3.join();
        int[][] p4 = mp4.join();
        int[][] p5 = mp5.join();
        int[][] p6 = mp6.join();
        int[][] p7 = mp7.join();

        int[][] c11 = sum(sum(p1, p4), sub(p7, p5));
        int[][] c12 = sum(p3, p5);
        int[][] c21 = sum(p2, p4);
        int[][] c22 = sum(sub(p1, p2), sum(p3, p6));

        return collect(c11, c12, c21, c22);
    }

    private static int[][] sum(int a[][], int b[][])
    {
        int n = a.length;
        int m = a[0].length;
        int[][] c = new int[n][m];
        for (int i =0; i < n; i++)
            for (int j = 0; j < m; j++)
                c[i][j] = a[i][j] + b[i][j];
        return c;
    }
    private static int[][] sub(int a[][], int b[][])
    {
        int n = a.length;
        int m = a[0].length;
        int[][] c = new int[n][m];
        for (int i =0; i < n; i++)
            for (int j = 0; j < m; j++)
                c[i][j] = a[i][j] - b[i][j];
        return c;
    }

    public static int[][] mul(int[][] a, int[][] b)
    {
        int rowsA = a.length;
        int columnsB = b[0].length;
        int ab = a[0].length;
        int[] columnB = new int[ab];
        int[][] c = new int[rowsA][columnsB];

        for (int j = 0; j< columnsB; j++)
        {
            for (int k = 0; k < ab; k++)
                columnB[k] = b[k][j];
            for (int i = 0; i < rowsA; i++)
            {
                int rowA[] = a[i];
                int sum = 0;
                for (int k = 0; k < ab; k++)
                    sum += rowA[k] * columnB[k];
                c[i][j] = sum;
            }
        }
        return c;
    }

    private static int log2(int x)
    {
        int res = 1;
        while ((x >>= 1) != 0)
            res++;
        return res;
    }
    private static int getDim(int[][] a, int[][] b)
    {
        return 1 << log2(Collections.max(Arrays.asList(a.length, a[0].length, b[0].length)));
    }
    private static int[][] addition2SquareMatrix(int[][] a, int n)
    {
        int[][] res = new int[n][n];
        for (int i = 0; i < a.length; i++)
            System.arraycopy(a[i], 0, res[i], 0, a[i].length);
        return res;
    }
    private static int[][] getSubM(int[][] a, int n, int m)
    {
        int[][] res = new int[n][m];
        for (int i = 0; i < n; i++)
            System.arraycopy(a[i], 0, res[i], 0, m);
        return res;
    }
    private static void split(int[][] a, int[][] a11, int[][] a12, int[][] a21, int[][] a22)
    {
        int n = a.length/2;
        for (int i = 0; i < n; i++)
        {
            System.arraycopy(a[i], 0, a11[i], 0, n);
            System.arraycopy(a[i], n, a12[i], 0, n);
            System.arraycopy(a[i + n], 0, a21[i], 0, n);
            System.arraycopy(a[i + n], n, a22[i], 0, n);
        }
    }
    private static int[][] collect(int[][] a11, int[][] a12, int[][] a21, int[][] a22)
    {
        int n = a11.length;
        int[][] a = new int[n << 1][n << 1];
        for (int i = 0; i < n; i++)
        {
            System.arraycopy(a11[i], 0, a[i], 0, n);
            System.arraycopy(a12[i], 0, a[i], n, n);
            System.arraycopy(a21[i], 0, a[i + n], 0, n);
            System.arraycopy(a22[i], 0, a[i + n], n, n);
        }
        return a;
    }
}
