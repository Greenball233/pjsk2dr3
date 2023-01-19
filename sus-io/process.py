import sys
import sus

if __name__ == "__main__":
    if len(sys.argv) > 2:
        print("Try to transform from sus to json")
        with open(sys.argv[1], "r") as fi, open(sys.argv[2], "w") as fo:
            try:
                score = sus.load(fi)
                json = score.to_json()
                fo.write(json)
                print("OK")
            except ValueError:
                print("Transform failed")
            except Exception:
                print("Transform failed")
    else:
        print("Error: Not Enough Arguments")
else:
    print("Illegal invoking")
