const fs = require('fs');
const path = require('path');

// Try different path representations
const paths = [
    // UTF-16 LE encoded (Windows native)
    'E:\\Desktop\\代码审查与缺陷解释系统\\概要设计说明书.docx',
    // Relative paths
    '..\\..\\..\\Desktop\\代码审查与缺陷解释系统\\概要设计说明书.docx',
    '.\\..\\Desktop\\代码审查与缺陷解释系统\\概要设计说明书.docx',
    'E:\\Desktop\\代码审查与缺陷解释系统\\概要设计说明书.docx'
];

const destPath = 'E:\\Desktop\\code\\doc.docx';

// Try to find and copy the file
async function tryPaths() {
    for (const p of paths) {
        console.log('Trying:', p);
        try {
            if (fs.existsSync(p)) {
                console.log('Found:', p);
                const data = fs.readFileSync(p);
                fs.writeFileSync(destPath, data);
                console.log('Copied! Size:', data.length, 'bytes');
                return true;
            } else {
                console.log('  File does not exist');
            }
        } catch (err) {
            console.error('  Error:', err.message);
        }
    }
    
    // List files in Desktop to see what's available
    console.log('\nListing Desktop contents:');
    try {
        const desktopPath = 'E:\\Desktop';
        const files = fs.readdirSync(desktopPath);
        files.forEach(f => {
            try {
                const fullPath = path.join(desktopPath, f);
                const stats = fs.statSync(fullPath);
                console.log(' ', f, stats.isDirectory() ? '[DIR]' : `[${stats.size}]`);
            } catch (e) {}
        });
    } catch (err) {
        console.error('Error listing Desktop:', err.message);
    }
    
    return false;
}

tryPaths().then(found => {
    if (!found) {
        console.log('\nCould not find the DOCX file');
    }
});
