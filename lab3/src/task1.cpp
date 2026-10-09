#include <clocale>
#include <iostream>

int main() {
    setlocale(LC_ALL, "Russian");
    const int MAX = 10;
    double a[MAX][MAX];
    int n, m;
    std::cout << "Введите n и m (не более 10): ";
    std::cin >> n >> m;
    std::cout << "Введите элементы матрицы:\n";
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < m; j++) {
            std::cin >> a[i][j];
        }
    }

    int firstNeg = -1, lastPos = -1;
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < m; j++) {
            int k = i * m + j;
            if (a[i][j] < 0 && firstNeg == -1) {
                firstNeg = k;
            }
            if (a[i][j] > 0) {
                lastPos = k;
            }
        }
    }

    if (firstNeg != -1) {
        std::cout << "Первый отрицательный элемент: " << a[firstNeg / m][firstNeg % m]
                  << ", порядковый номер " << firstNeg + 1 << "\n";
    } else {
        std::cout << "Отрицательных элементов нет\n";
    }
    if (lastPos != -1) {
        std::cout << "Последний положительный элемент: " << a[lastPos / m][lastPos % m]
                  << ", порядковый номер " << lastPos + 1 << "\n";
    } else {
        std::cout << "Положительных элементов нет\n";
    }
    return 0;
}
