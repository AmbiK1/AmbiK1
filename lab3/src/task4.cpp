#include <cctype>
#include <clocale>
#include <cstring>
#include <iostream>

int main() {
    setlocale(LC_ALL, "Russian");
    char text[256] = "";
    char result[256] = "";
    int k;
    std::cout << "Введите текст: ";
    std::cin.getline(text, 256);
    std::cout << "Введите длину слова: ";
    std::cin >> k;

    int i = 0, r = 0;
    while (text[i] != '\0') {
        if (isalpha((unsigned char)text[i])) {
            int start = i;
            while (isalpha((unsigned char)text[i])) {
                i++;
            }
            int len = i - start;
            bool consonant = strchr("aeiouyAEIOUY", text[start]) == NULL;
            if (len == k && consonant) {
                while (text[i] == ' ') {
                    i++;
                }
            } else {
                for (int j = start; j < i; j++) {
                    result[r] = text[j];
                    r++;
                }
            }
        } else {
            result[r] = text[i];
            r++;
            i++;
        }
    }
    result[r] = '\0';

    std::cout << "Результат: " << result << "\n";
    return 0;
}
