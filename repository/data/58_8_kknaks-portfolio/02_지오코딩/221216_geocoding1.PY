import requests
import openpyxl as xls
from openpyxl import load_workbook
import pandas as pd


#api 호출함수
def call_api(address,key):
    apiurl = "http://api.vworld.kr/req/address?"
    params = {
    "service": "address",
    "request": "getcoord",
    "crs": f"epsg:{epsg}",
    "address": address,
    "format": "json",
    "type": "road",
    "key": key
    }
    
    response = requests.get(apiurl, params=params)
    return response

#좌표 추출
def disc(result):
    disc = result.get('response').get('status')
    if disc == 'OK':
        geo_x = (result.get('response').get('result').get('point').get('x'))
        geo_y = (result.get('response').get('result').get('point').get('y'))
    else :
        geo_x = '-'
        geo_y = '-'

    return geo_x, geo_y


#엑셀호출
def call_excel():
    read_xlsx = load_workbook(r"./data/sample.xlsx")
    read_sheet = read_xlsx.active
    name_col = read_sheet['A']
    address = []
    for row in name_col:
        address.append(row.value)
    return address
    

#지오코딩 
def geocoding():
    geocode = pd.DataFrame()
    address = call_excel()
    for name in address:
        response = call_api(name, key)

        if response.status_code == 200:
            result = response.json()
            geo_x,geo_y = disc(result)

            temp = pd.DataFrame({
                'address' : name,
                'geo_x' : geo_x,
                'geo_y' : geo_y
                }, index = [0])

            geocode = geocode.append(temp)
    
    return geocode

key = input("키값을 입력 하세요 : ")
epsg = input("epsgf를 입력 하세요 : ")

geo = geocoding()
print(geo)