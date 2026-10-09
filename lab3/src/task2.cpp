#include <clocale>
#include <iomanip>
#include <iostream>

int main() {
    setlocale(LC_ALL, "Russian");
    const int MAX = 10;
    int a[MAX], b[MAX][MAX];
    int n;
    std::cout << "Введите n (не более 10): ";
    std::cin >> n;
    std::cout << "Введите элементы массива: ";
    for (int i = 0; i < n; i++) {
        std::cin >> a[i];
    }

    for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
            b[i][j] = a[(i + j) % n];
        }
    }

    std::cout << "Полученная матрица:\n";
    for (int i = 0; i < n; i++) {
        for (int j = 0; j < n; j++) {
            std::cout << std::setw(5) << b[i][j];
        }
        std::cout << "\n";
    }
    return 0;
}
