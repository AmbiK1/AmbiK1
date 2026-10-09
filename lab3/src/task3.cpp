#include <clocale>
#include <cstdlib>
#include <iomanip>
#include <iostream>
#include <utility>

int main() {
    setlocale(LC_ALL, "Russian");
    const int MAX = 10;
    int a[MAX][MAX], ch[MAX];
    int n, m;
    std::cout << "Введите n и m (не более 10): ";
    std::cin >> n >> m;
    std::cout << "Введите элементы матрицы:\n";
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < m; j++) {
            std::cin >> a[i][j];
        }
    }

    for (int j = 0; j < m; j++) {
        ch[j] = 0;
        for (int i = 0; i < n; i++) {
            if (a[i][j] < 0 && a[i][j] % 2 != 0) {
                ch[j] = ch[j] + abs(a[i][j]);
            }
        }
    }

    for (int p = 0; p < m - 1; p++) {
        for (int j = 0; j < m - 1 - p; j++) {
            if (ch[j] > ch[j + 1]) {
                std::swap(ch[j], ch[j + 1]);
                for (int i = 0; i < n; i++) {
                    std::swap(a[i][j], a[i][j + 1]);
                }
            }
        }
    }

    std::cout << "Характеристики столбцов:\n";
    for (int j = 0; j < m; j++) {
        std::cout << std::setw(5) << ch[j];
    }
    std::cout << "\nМатрица после перестановки столбцов:\n";
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < m; j++) {
            std::cout << std::setw(5) << a[i][j];
        }
        std::cout << "\n";
    }
    return 0;
}
