#include <clocale>
#include <iostream>

int main() {
    setlocale(LC_ALL, "Russian");
    int n;
    std::cout << "Введите количество элементов n: ";
    std::cin >> n;

    double* a = new double[n];
    std::cout << "Введите элементы массива: ";
    for (int i = 0; i < n; i++) {
        std::cin >> a[i];
    }

    double negSum = 0, posSum = 0;
    for (int i = 0; i < n; i++) {
        if (a[i] < 0) {
            negSum = negSum + a[i];
        } else if (a[i] > 0) {
            posSum = posSum + a[i];
        }
    }

    std::cout << "Сумма отрицательных чисел: " << negSum << "\n";
    std::cout << "Сумма положительных чисел: " << posSum << "\n";
    delete[] a;
    return 0;
}
