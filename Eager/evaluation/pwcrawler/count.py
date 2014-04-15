if __name__ == '__main__':
    fh = open('pweb.txt', 'r')
    lines = fh.readlines()
    fh.close()
    mashups = set()
    for line in lines:
        if line.startswith('  -->'):
            mashups.add(line)
    print 'Mashups:', len(mashups) 
